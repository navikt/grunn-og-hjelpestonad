package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Tidsrommet [vedtak] gjelder i. Tom betyr at perioden er løpende. */
data class GjeldendeInfotrygdPeriode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val stonadId: Long,
    val vedtak: InfotrygdVedtakMedArv,
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

        // Ett vedtak om gangen: tilTidslinje() på alle vedtakene samtidig kaster ved overlapp.
        return perioder
            .map { it.tilTidslinje() }
            .reduce { eldre, nyere -> nyere.kombinerMed(eldre) { ny, gammel -> ny ?: gammel } }
            .slåSammenLikePerioder()
            .tilPerioder()
            .filtrerIkkeNull()
            .map {
                GjeldendeInfotrygdPeriode(
                    fom = checkNotNull(it.fom) { "Gjeldende periode mangler fom" },
                    tom = it.tom,
                    stonadId = it.verdi.stonadId,
                    vedtak = it.verdi.vedtak,
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

        val datoOpphør = stønad.datoOpphor ?: return Periode(VedtakPåStønad(stønad.stonadId, this), datoInnvFom, datoInnvTom)

        // Dersom det er et opphør på samme dag som vedtaket starter, regner vi det som at hele vedtaket er opphørt, og tar det ikke med.
        if (datoInnvFom == datoOpphør) return null

        val tom = listOfNotNull(datoInnvTom, faktiskOpphørsdato(datoOpphør)).min()
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId starter ${datoInnvFom.format(DATOFORMAT)}, etter at stønaden opphørte " +
                    "${datoOpphør.format(DATOFORMAT)}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
            )
        }
        return Periode(VedtakPåStønad(stønad.stonadId, this), datoInnvFom, tom)
    }

    private fun InfotrygdVedtakMedArv.validerAtTomKommerEtterFom() {
        val tom = datoInnvTom ?: return
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId i Infotrygd har innvilget tom ${tom.format(DATOFORMAT)}, som ikke er etter " +
                    "innvilget fom ${datoInnvFom.format(DATOFORMAT)}.",
            )
        }
    }

    private fun validerLøpendeVedtakPåHøystÉnStønad(perioder: List<Periode<VedtakPåStønad>>) {
        val stønaderMedLøpendeVedtak =
            perioder
                .filter { it.tom == null }
                .map { it.verdi.stonadId }
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
        val tom = sisteVedtak.datoInnvTom ?: return

        // Ønsker bare å kaste feil dersom dette treffer saker som nylig er avsluttet.
        if (stønad.datoOpphor == null && tom >= DATOGRENSE_FOR_MIGRERING) {
            throw UgyldigInput(
                "Stønad ${stønad.stonadId} er ikke opphørt, men siste vedtak ${sisteVedtak.vedtakId} sluttet ${tom.format(DATOFORMAT)}. " +
                    "Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.",
            )
        }
    }

    /** Opphør den 1. i måneden betyr at stønaden gjaldt ut måneden før. */
    private fun faktiskOpphørsdato(datoOpphor: LocalDate): LocalDate = if (datoOpphor.dayOfMonth == 1) datoOpphor.minusDays(1) else datoOpphor

    private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Se ADR-0013. EF bruker 2019-01-01. Vi bør se på denne på nytt når vi begynner med migrering */
    private val DATOGRENSE_FOR_MIGRERING: LocalDate = LocalDate.of(2026, 1, 1)

    private data class VedtakPåStønad(
        val stonadId: Long,
        val vedtak: InfotrygdVedtakMedArv,
    )
}
