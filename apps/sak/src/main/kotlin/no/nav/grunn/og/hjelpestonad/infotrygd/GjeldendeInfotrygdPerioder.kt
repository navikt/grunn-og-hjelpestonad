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

/**
 * Hvert vedtak blir én periode fra datoInnvFom til datoInnvTom. Vedtakene legges på tidslinjen etter stigende
 * vedtakId, og et vedtak overskriver de eldre i hele sin periode, på tvers av stønadene. Et vedtak med tom
 * før eller lik fom gir [UgyldigInput].
 */
object GjeldendeInfotrygdPerioder {
    fun utled(stønader: List<InfotrygdStonadMedArv>): List<GjeldendeInfotrygdPeriode> {
        val tidslinjer =
            stønader
                .flatMap { stønad -> stønad.vedtak.map { VedtakPåStønad(stønad.stonadId, it) } }
                .sortedBy { it.vedtak.vedtakId }
                .onEach { it.vedtak.validerPeriode() }
                .map { Periode(it, it.vedtak.datoInnvFom, it.vedtak.datoInnvTom).tilTidslinje() }

        if (tidslinjer.isEmpty()) return emptyList()

        // Ett vedtak om gangen: tilTidslinje() på alle vedtakene samtidig kaster ved overlapp.
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

    private fun InfotrygdVedtakMedArv.validerPeriode() {
        val tom = datoInnvTom ?: return
        if (tom <= datoInnvFom) {
            throw UgyldigInput(
                "Vedtak $vedtakId i Infotrygd har innvilget tom ${tom.format(DATOFORMAT)}, som ikke er etter " +
                    "innvilget fom ${datoInnvFom.format(DATOFORMAT)}.",
            )
        }
    }

    private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private data class VedtakPåStønad(
        val stonadId: Long,
        val vedtak: InfotrygdVedtakMedArv,
    )
}
