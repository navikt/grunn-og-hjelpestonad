package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelData as AndelForUtbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Oppdragsgrunnlag(
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
    val personIdent: String,
    val saksbehandlerId: String,
    val vedtaksdato: LocalDate,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val utbetalingsandelerForrigeBehandling: List<Utbetalingsandel> = emptyList(),
    val sisteAndelIHverKjedeForFagsak: List<Utbetalingsandel> = emptyList(),
)

/**
 * @param utbetalingsandeler alle løpende andeler etter behandlingen, med kjedefeltene. Lagres i `utbetalingsandel`, og blir
 * forrige andeler for neste behandling.
 */
data class BeregnetOppdrag(
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val utbetalingsandeler: List<Utbetalingsandel>,
) {
    /** Usann når generatoren ikke fant noen endring mot forrige behandling. Da skal ingenting sendes til OS. */
    val harEndringFraForrigeBehandling: Boolean get() = utbetalingsoppdrag.utbetalingsperiode.isNotEmpty()
}

/**
 * Lager oppdraget for en behandling, kjedet mot andelene som allerede er sendt på fagsaken.
 *
 * Kjedingen gjøres av `familie-utbetalingsgenerator`: uendrede andeler beholder `periodeId`, endrede og nye andeler får
 * neste ledige `periodeId`, og kjeden opphøres fra første endring. Alle andeler får personens gjeldende ident, også
 * de forrige, slik at et identbytte ikke ser ut som en ny kjede.
 *
 * `opphørAlleKjederFra` er alltid `null`. Den skal bare settes i «Endre migreringsdato»-behandlinger (ADR-0011), som
 * ikke finnes ennå.
 */
fun lagUtbetalingsoppdrag(grunnlag: Oppdragsgrunnlag): BeregnetOppdrag {
    val nyeAndeler = grunnlag.andelerTilkjentYtelse.filter { tilUtbetalingsbeløp(it.beløp) != 0 }
    if (nyeAndeler.isEmpty() && grunnlag.sisteAndelIHverKjedeForFagsak.isEmpty()) {
        throw UgyldigInput("Oppdrag krever minst én andel med beløp ulik 0, eller en tidligere kjede som skal opphøres")
    }

    val beregnet =
        Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon =
                Behandlingsinformasjon(
                    saksbehandlerId = grunnlag.saksbehandlerId,
                    behandlingId = grunnlag.behandlingId.toString(),
                    eksternBehandlingId = grunnlag.eksternBehandlingId,
                    eksternFagsakId = grunnlag.eksternFagsakId,
                    fagsystem = GrunnOgHjelpFagsystem.IT28,
                    personIdent = grunnlag.personIdent,
                    vedtaksdato = grunnlag.vedtaksdato,
                    opphørAlleKjederFra = null,
                ),
            nyeAndeler = nyeAndeler.map { it.tilAndelForUtbetalingsgenerator(grunnlag.personIdent) },
            forrigeAndeler = grunnlag.utbetalingsandelerForrigeBehandling.map { it.tilAndelForUtbetalingsgenerator(grunnlag.personIdent) },
            sisteAndelPerKjede =
                grunnlag.sisteAndelIHverKjedeForFagsak
                    .map { it.tilAndelForUtbetalingsgenerator(grunnlag.personIdent) }
                    .associateBy { IdentOgType(it.personIdent, it.type) },
        )

    // Generatoren gir tilbake én andel per ny andel, med samme id, også for andeler som er uendret fra forrige
    // behandling. Periodene hentes fra tilkjent ytelse, kjedefeltene fra generatoren.
    val nyeAndelerPerId = nyeAndeler.associateBy { it.id.toString() }
    val andeler =
        beregnet.andeler.map { andelMedPeriodeId ->
            val andel = nyeAndelerPerId[andelMedPeriodeId.id] ?: error("Generatoren ga ukjent andel=${andelMedPeriodeId.id}")
            Utbetalingsandel(
                fom = andel.fom,
                tom = andel.tom,
                beløp = tilUtbetalingsbeløp(andel.beløp),
                ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
                periodeId = andelMedPeriodeId.periodeId,
                forrigePeriodeId = andelMedPeriodeId.forrigePeriodeId,
                kildeBehandlingId = UUID.fromString(andelMedPeriodeId.kildeBehandlingId),
            )
        }
    check(andeler.size == nyeAndeler.size) { "Generatoren ga ${andeler.size} andeler, forventet ${nyeAndeler.size}" }

    return BeregnetOppdrag(utbetalingsoppdrag = beregnet.utbetalingsoppdrag, utbetalingsandeler = andeler.sortedBy { it.fom })
}

private fun AndelTilkjentYtelse.tilAndelForUtbetalingsgenerator(personIdent: String) =
    AndelForUtbetalingsgenerator(
        id = id.toString(),
        fom = YearMonth.from(fom),
        tom = YearMonth.from(tom),
        beløp = tilUtbetalingsbeløp(beløp),
        personIdent = personIdent,
        type = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
        periodeId = null,
        forrigePeriodeId = null,
        kildeBehandlingId = null,
    )

private fun Utbetalingsandel.tilAndelForUtbetalingsgenerator(personIdent: String) =
    AndelForUtbetalingsgenerator(
        id = id.toString(),
        fom = YearMonth.from(fom),
        tom = YearMonth.from(tom),
        beløp = beløp,
        personIdent = personIdent,
        type = ytelsestype,
        periodeId = periodeId,
        forrigePeriodeId = forrigePeriodeId,
        kildeBehandlingId = kildeBehandlingId.toString(),
    )
