package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.felles.NORSK_DATOFORMAT
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdDelytelseResponse
import java.math.BigDecimal
import java.time.LocalDate

/** Tidsrommet [vedtak] gjelder i. Tom betyr at perioden er løpende. */
data class GjeldendeInfotrygdPeriode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val stønadId: Long,
    val vedtak: InfotrygdVedtakMedArv,
    val infotrygdBeløp: BigDecimal,
)

object GjeldendeInfotrygdPerioder {
    fun utled(stønader: List<InfotrygdStonadMedArv>): List<GjeldendeInfotrygdPeriode> {
        val perioder =
            stønader
                .flatMap { stønad -> stønad.vedtak.map { vedtak -> stønad to vedtak } }
                .sortedBy { (_, vedtak) -> vedtak.vedtakId }
                .mapNotNull { (stønad, vedtak) -> vedtak.tilPeriode(stønad) }

        validerLøpendeVedtakPåHøystÉnStønad(perioder)
        validerIkkeVedtakAvsluttetMenStønadErLøpende(stønader)

        if (perioder.isEmpty()) return emptyList()

        val gjeldendePerioder =
            perioder
                .map { it.tilTidslinje() }
                .reduce { eldre, nyere -> nyere.kombinerMed(eldre) { ny, gammel -> ny ?: gammel } }
                .tilPerioder()
                .filtrerIkkeNull()

        return gjeldendePerioder.mapIndexed { indeks, periode ->
            val delytelse = periode.verdi.hentValidertDelytelse(erISistePeriode = indeks == gjeldendePerioder.lastIndex)
            GjeldendeInfotrygdPeriode(
                fom = checkNotNull(periode.fom) { "Gjeldende periode mangler fom" },
                tom = periode.tom,
                stønadId = periode.verdi.stønadId,
                vedtak = periode.verdi.vedtak,
                infotrygdBeløp = delytelse.beløp,
            )
        }
    }

    // Det er ingen tilfeller der vedtakId er mindre mens tidspunktReg er større enn et annet vedtak i infotrygd dev.
    fun antallVedtakUtenforRekkefølge(stønader: List<InfotrygdStonadMedArv>): Int =
        stønader
            .flatMap { it.vedtak }
            .sortedBy { it.vedtakId }
            .zipWithNext()
            .count { (forrige, neste) -> neste.tidspunktReg < forrige.tidspunktReg }

    private fun InfotrygdVedtakMedArv.tilPeriode(stønad: InfotrygdStonadMedArv): Periode<VedtakPåStønad>? {
        this.validerAtTomKommerEtterFom()

        val datoOpphør = stønad.datoOpphør ?: return Periode(VedtakPåStønad(stønad.stønadId, this, datoInnvTom), datoInnvFom, datoInnvTom)

        // Dersom det er et opphør på samme dag som vedtaket starter, regner vi det som at hele vedtaket er opphørt, og tar det ikke med.
        if (datoInnvFom == datoOpphør) return null

        val tom = listOfNotNull(datoInnvTom, faktiskOpphørsdato(datoOpphør)).min()
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId starter ${datoInnvFom.format(NORSK_DATOFORMAT)}, etter at stønaden opphørte " +
                    "${datoOpphør.format(NORSK_DATOFORMAT)}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
            )
        }
        return Periode(VedtakPåStønad(stønad.stønadId, this, tom), datoInnvFom, tom)
    }

    private fun InfotrygdVedtakMedArv.validerAtTomKommerEtterFom() {
        val tom = datoInnvTom ?: return
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId i Infotrygd har innvilget tom ${tom.format(NORSK_DATOFORMAT)}, som ikke er etter " +
                    "innvilget fom ${datoInnvFom.format(NORSK_DATOFORMAT)}.",
            )
        }
    }

    private fun validerLøpendeVedtakPåHøystÉnStønad(perioder: List<Periode<VedtakPåStønad>>) {
        val stønaderMedLøpendeVedtak =
            perioder
                .filter { it.tom == null }
                .map { it.verdi.stønadId }
                .distinct()
                .sorted()
        if (stønaderMedLøpendeVedtak.size >= 2) {
            throw UgyldigInput(
                "Personen har løpende vedtak på flere stønader i Infotrygd (${stønaderMedLøpendeVedtak.joinToString(", ")}). " +
                    "Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.",
            )
        }
    }

    private fun validerIkkeVedtakAvsluttetMenStønadErLøpende(stønader: List<InfotrygdStonadMedArv>) {
        val (stønad, sisteVedtak) =
            stønader
                .flatMap { stønad -> stønad.vedtak.map { vedtak -> stønad to vedtak } }
                .maxByOrNull { (_, vedtak) -> vedtak.vedtakId } ?: return
        val vedtakTom = sisteVedtak.datoInnvTom ?: return

        // Ønsker bare å kaste feil dersom dette treffer saker som nylig er avsluttet.
        if (stønad.erLøpende() && !sisteVedtak.erLøpende() && vedtakTom >= DATOGRENSE_FOR_MIGRERING) {
            throw UgyldigInput(
                "Stønad ${stønad.stønadId} er ikke opphørt, men siste vedtak ${sisteVedtak.vedtakId} sluttet ${vedtakTom.format(NORSK_DATOFORMAT)}. " +
                    "Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.",
            )
        }
    }

    /** Opphør den 1. i måneden betyr at stønaden gjaldt ut måneden før. */
    private fun faktiskOpphørsdato(datoOpphør: LocalDate): LocalDate = if (datoOpphør.dayOfMonth == 1) datoOpphør.minusDays(1) else datoOpphør

    private fun VedtakPåStønad.hentValidertDelytelse(erISistePeriode: Boolean): InfotrygdDelytelseResponse {
        if (vedtak.delytelser.size > 1) {
            throw UgyldigInput(
                "Vedtak ${vedtak.vedtakId} har ${vedtak.delytelser.size} delytelser i Infotrygd, ikke én. " +
                    "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
            )
        }
        val delytelse =
            vedtak.delytelser.firstOrNull()
                ?: throw UgyldigInput(
                    "Vedtak ${vedtak.vedtakId} gjelder ${periodetekst(vedtak.datoInnvFom, tom)}, men har ingen delytelse med beløpet i Infotrygd. " +
                        "Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
                )
        // Vedtaket, ikke delytelsen, avgjør tidsrommet perioden gjelder for. Delytelsen trengs kun for beløpet. Starter delytelsen senere eller slutter den
        // tidligere enn vedtaket, vet vi ikke beløpet for resten av vedtaket, og vi kaster derfor feil. At den slutter
        // senere, skyldes kuttet ved opphør: Infotrygd setter tom på vedtaket, men ikke på delytelsen. Det godtar vi i
        // tidligere perioder. I siste periode, som migreringen bygger på, krever vi foreløpig lik periode.
        val godkjentPeriode =
            if (erISistePeriode) {
                delytelse.fom == vedtak.datoInnvFom && delytelse.tom == tom
            } else {
                delytelse.fom <= vedtak.datoInnvFom && delytelse.tom.let { it == null || (tom != null && it >= tom) }
            }
        if (!godkjentPeriode) {
            throw UgyldigInput(
                "Vedtak ${vedtak.vedtakId} gjelder ${periodetekst(vedtak.datoInnvFom, tom)}, men delytelsen med beløpet i Infotrygd " +
                    "gjelder ${periodetekst(delytelse.fom, delytelse.tom)}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
            )
        }
        return delytelse
    }

    private fun periodetekst(
        fom: LocalDate,
        tom: LocalDate?,
    ): String = if (tom == null) "fra ${fom.format(DATOFORMAT)}" else "${fom.format(DATOFORMAT)}–${tom.format(DATOFORMAT)}"

    private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Se ADR-0013. EF bruker 2019-01-01. Vi bør se på denne på nytt når vi begynner med migrering */
    private val DATOGRENSE_FOR_MIGRERING: LocalDate = LocalDate.of(2026, 1, 1)

    private data class VedtakPåStønad(
        val stønadId: Long,
        val vedtak: InfotrygdVedtakMedArv,
        /** Tom etter kuttet ved opphør. Null betyr at vedtaket er løpende. */
        val tom: LocalDate?,
    )
}
