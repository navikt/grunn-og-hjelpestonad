/*
 * MIT License
 *
 * Copyright (c) 2019 NAV
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator

import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.BeståendeAndelerBeregner.finnBeståendeAndeler
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.OppdragBeregnerUtil.validerAndeler
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.AndelData
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.AndelMedPeriodeId
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.IdentOgType
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.groupByIdentOgType
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.representererSammePeriodeSom
import no.nav.grunn.og.hjelpestonad.oppdrag.utbetalingsgenerator.domain.uten0beløp
import java.time.YearMonth

/**
 * Midlertidig kopi fra navikt/familie-utbetalingsgenerator, commit 384875f, PR #193.
 * Fjern pakken og gjeninnfør bibliotekavhengigheten når null-støtten er publisert.
 * Long-ID-overlasten er utelatt. Beregningslogikken er beholdt.
 */
class Utbetalingsgenerator {
    fun lagUtbetalingsoppdrag(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeAndeler: List<AndelData>,
        forrigeAndeler: List<AndelData>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
    ): BeregnetUtbetalingsoppdrag {
        validerAndeler(behandlingsinformasjon, forrigeAndeler, nyeAndeler, sisteAndelPerKjede)
        val nyeAndelerGruppert = nyeAndeler.groupByIdentOgType()
        val forrigeKjeder = forrigeAndeler.groupByIdentOgType()

        return lagUtbetalingsoppdrag(
            nyeAndeler = nyeAndelerGruppert,
            forrigeKjeder = forrigeKjeder,
            sisteAndelPerKjede = sisteAndelPerKjede,
            behandlingsinformasjon = behandlingsinformasjon,
        )
    }

    private fun lagUtbetalingsoppdrag(
        nyeAndeler: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): BeregnetUtbetalingsoppdrag {
        val nyeKjeder = lagNyeKjeder(nyeAndeler, forrigeKjeder, sisteAndelPerKjede, behandlingsinformasjon)

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                saksbehandlerId = behandlingsinformasjon.saksbehandlerId,
                kodeEndring = kodeEndring(sisteAndelPerKjede),
                fagSystem = behandlingsinformasjon.fagsystem.kode,
                saksnummer = behandlingsinformasjon.eksternFagsakId.toString(),
                aktoer = behandlingsinformasjon.personIdent,
                utbetalingsperiode = utbetalingsperioder(behandlingsinformasjon, nyeKjeder),
                gOmregning = behandlingsinformasjon.erGOmregning,
            )

        return BeregnetUtbetalingsoppdrag(
            utbetalingsoppdrag,
            lagAndelerMedPeriodeId(behandlingsinformasjon, nyeKjeder),
        )
    }

    private fun lagNyeKjeder(
        nyeKjeder: Map<IdentOgType, List<AndelData>>,
        forrigeKjeder: Map<IdentOgType, List<AndelData>>,
        sisteAndelPerKjede: Map<IdentOgType, AndelData>,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): List<ResultatForKjede> {
        val alleIdentOgTyper = nyeKjeder.keys + forrigeKjeder.keys
        var sistePeriodeId = sisteAndelPerKjede.values.mapNotNull { it.periodeId }.maxOrNull() ?: -1
        return alleIdentOgTyper.sorted().map { identOgType ->
            val forrigeAndeler = forrigeKjeder[identOgType] ?: emptyList()
            val nyeAndeler = nyeKjeder[identOgType] ?: emptyList()
            val sisteAndel = sisteAndelPerKjede[identOgType]
            val opphørsdato = finnOpphørsdato(forrigeAndeler, nyeAndeler, sisteAndel, behandlingsinformasjon)

            val nyKjede =
                beregnNyKjede(
                    forrigeAndeler.uten0beløp(),
                    nyeAndeler.uten0beløp(),
                    sisteAndel,
                    sistePeriodeId,
                    opphørsdato,
                )
            sistePeriodeId = nyKjede.sistePeriodeId
            nyKjede
        }
    }

    private fun finnOpphørsdato(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
        sisteAndel: AndelData?,
        behandlingsinformasjon: Behandlingsinformasjon,
    ): YearMonth? {
        if (sisteAndel == null) {
            return null
        }
        if (behandlingsinformasjon.opphørKjederFraFørsteUtbetaling) {
            return forrigeAndeler.uten0beløp().minOfOrNull { it.fom }
        }
        return behandlingsinformasjon.opphørAlleKjederFra ?: finnOpphørsdatoPga0Beløp(forrigeAndeler, nyeAndeler)
    }

    private fun finnOpphørsdatoPga0Beløp(
        forrigeAndeler: List<AndelData>,
        nyeAndeler: List<AndelData>,
    ): YearMonth? {
        val forrigeFørsteAndel = forrigeAndeler.firstOrNull()
        val nyFørsteAndel = nyeAndeler.firstOrNull()
        if (
            forrigeFørsteAndel != null &&
            nyFørsteAndel != null &&
            nyFørsteAndel.beløp == 0 &&
            nyFørsteAndel.fom < forrigeFørsteAndel.fom
        ) {
            return nyFørsteAndel.fom
        }
        return null
    }

    private fun utbetalingsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<Utbetalingsperiode> {
        val opphørsperioder = lagOpphørsperioder(behandlingsinformasjon, nyeKjeder.mapNotNull { it.opphørsandel })
        val nyePerioder = lagNyePerioder(behandlingsinformasjon, nyeKjeder.flatMap { it.nyeAndeler })
        return opphørsperioder + nyePerioder
    }

    private fun lagAndelerMedPeriodeId(
        behandlingsinformasjon: Behandlingsinformasjon,
        nyeKjeder: List<ResultatForKjede>,
    ): List<AndelMedPeriodeId> =
        nyeKjeder.flatMap { nyKjede ->
            val beståendeAndelerMedPeriodeId =
                nyKjede.beståendeAndeler.map { beståendeAndel ->
                    AndelMedPeriodeId(
                        andel = beståendeAndel,
                        nyKildeBehandlingId =
                            bestemKildeBehandlingIdForBeståendeAndel(
                                beståendeAndel = beståendeAndel,
                                opphørsandel = nyKjede.opphørsandel?.first,
                                inneværendeBehandlingId = behandlingsinformasjon.behandlingId,
                            ),
                    )
                }
            val nyeAndelerMedPeriodeId =
                nyKjede.nyeAndeler.map { nyAndel ->
                    AndelMedPeriodeId(
                        andel = nyAndel,
                        nyKildeBehandlingId = behandlingsinformasjon.behandlingId,
                    )
                }
            beståendeAndelerMedPeriodeId + nyeAndelerMedPeriodeId
        }

    private fun bestemKildeBehandlingIdForBeståendeAndel(
        beståendeAndel: AndelData,
        opphørsandel: AndelData?,
        inneværendeBehandlingId: String,
    ): String? {
        val beståendeAndelErOpphørsandel = opphørsandel?.representererSammePeriodeSom(beståendeAndel) ?: false

        return if (beståendeAndelErOpphørsandel) {
            inneværendeBehandlingId
        } else {
            null
        }
    }

    private fun kodeEndring(sisteAndelMedPeriodeId: Map<IdentOgType, AndelData>) = if (sisteAndelMedPeriodeId.isEmpty()) KodeEndring.NY else KodeEndring.ENDR

    private fun beregnNyKjede(
        forrige: List<AndelData>,
        nye: List<AndelData>,
        sisteAndel: AndelData?,
        periodeId: Long,
        opphørsdato: YearMonth?,
    ): ResultatForKjede {
        val beståendeAndeler = finnBeståendeAndeler(forrige, nye, opphørsdato)
        val nyeAndeler = nye.subList(beståendeAndeler.andeler.size, nye.size)

        val nyeAndelerMedPeriodeId = nyeAndelerMedPeriodeId(nyeAndeler, periodeId, sisteAndel)
        return ResultatForKjede(
            beståendeAndeler = beståendeAndeler.andeler,
            nyeAndeler = nyeAndelerMedPeriodeId.first,
            opphørsandel =
                beståendeAndeler.opphørFra?.let {
                    Pair(sisteAndel ?: error("Må ha siste andel for å kunne opphøre"), it)
                },
            sistePeriodeId = nyeAndelerMedPeriodeId.second,
        )
    }

    private fun nyeAndelerMedPeriodeId(
        nyeAndeler: List<AndelData>,
        periodeId: Long,
        sisteAndel: AndelData?,
    ): Pair<List<AndelData>, Long> {
        var gjeldendePeriodeId = periodeId
        var forrigePeriodeId = sisteAndel?.periodeId
        val nyeAndelerMedPeriodeId =
            nyeAndeler.map { andelData ->
                gjeldendePeriodeId += 1
                val nyAndel = andelData.copy(periodeId = gjeldendePeriodeId, forrigePeriodeId = forrigePeriodeId)
                forrigePeriodeId = gjeldendePeriodeId
                nyAndel
            }
        return Pair(nyeAndelerMedPeriodeId, gjeldendePeriodeId)
    }

    private fun lagOpphørsperioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<Pair<AndelData, YearMonth>>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal =
            UtbetalingsperiodeMal(
                behandlingsinformasjon = behandlingsinformasjon,
                erEndringPåEksisterendePeriode = true,
            )

        return andeler.map {
            utbetalingsperiodeMal.lagPeriodeFraAndel(it.first, opphørKjedeFom = it.second)
        }
    }

    private fun lagNyePerioder(
        behandlingsinformasjon: Behandlingsinformasjon,
        andeler: List<AndelData>,
    ): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(behandlingsinformasjon = behandlingsinformasjon)
        return andeler.map { utbetalingsperiodeMal.lagPeriodeFraAndel(it) }
    }
}
