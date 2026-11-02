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
        val tidslinjer =
            stønader
                .flatMap { stønad -> stønad.vedtak.map { vedtak -> stønad to vedtak } }
                .sortedBy { (_, vedtak) -> vedtak.vedtakId }
                .map { (stønad, vedtak) -> vedtak.tilPeriode(stønad).tilTidslinje() }

        if (tidslinjer.isEmpty()) return emptyList()

        return tidslinjer
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

    /**
     * Antall vedtak som er registrert før vedtaket med nærmeste lavere vedtakId. Brukes til å logge om
     * antakelsen bak rekkefølgen, at vedtakId stiger med registreringstidspunktet, holder.
     */
    fun antallVedtakUtenforRekkefølge(stønader: List<InfotrygdStonadMedArv>): Int =
        stønader
            .flatMap { it.vedtak }
            .sortedBy { it.vedtakId }
            .zipWithNext()
            .count { (forrige, neste) -> neste.tidspunktReg < forrige.tidspunktReg }

    private fun InfotrygdVedtakMedArv.tilPeriode(stønad: InfotrygdStonadMedArv): Periode<VedtakPåStønad> {
        validerInnvilgetPeriode()

        val datoOpphor = stønad.datoOpphor ?: return Periode(VedtakPåStønad(stønad.stonadId, this), datoInnvFom, datoInnvTom)
        val tom = listOfNotNull(datoInnvTom, faktiskOpphørsdato(datoOpphor)).min()
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId starter ${datoInnvFom.format(DATOFORMAT)}, etter at stønaden opphørte " +
                    "${datoOpphor.format(DATOFORMAT)}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.",
            )
        }
        return Periode(VedtakPåStønad(stønad.stonadId, this), datoInnvFom, tom)
    }

    private fun InfotrygdVedtakMedArv.validerInnvilgetPeriode() {
        val tom = datoInnvTom ?: return
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId i Infotrygd har innvilget tom ${tom.format(DATOFORMAT)}, som ikke er etter " +
                    "innvilget fom ${datoInnvFom.format(DATOFORMAT)}.",
            )
        }
    }

    /** Opphør den 1. i måneden betyr at stønaden gjaldt ut måneden før. */
    private fun faktiskOpphørsdato(datoOpphor: LocalDate): LocalDate = if (datoOpphor.dayOfMonth == 1) datoOpphor.minusDays(1) else datoOpphor

    private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private data class VedtakPåStønad(
        val stonadId: Long,
        val vedtak: InfotrygdVedtakMedArv,
    )
}
