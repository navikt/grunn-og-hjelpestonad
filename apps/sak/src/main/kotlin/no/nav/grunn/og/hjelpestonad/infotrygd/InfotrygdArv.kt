package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdDelytelseResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdEvaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdGhResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdVedtakResponse
import java.time.LocalDate
import java.time.LocalDateTime

/** Vedtaket en arvet verdi er hentet fra. */
data class InfotrygdKildevedtak(
    val vedtakId: Long,
    val vedtakstidspunkt: LocalDateTime,
)

/** Samme felt som [InfotrygdStonadResponse], med vedtak som har fått arv. */
data class InfotrygdStonadMedArv(
    val stonadId: Long,
    val personnr: String,
    val kodeRutine: String,
    val datoStart: LocalDate,
    val kodeOpphor: String?,
    val datoOpphor: LocalDate?,
    val oppdragId: Long?,
    val tidspunktOpphort: LocalDateTime?,
    val tidspunktReg: LocalDateTime?,
    val brukerid: String?,
    val vedtak: List<InfotrygdVedtakMedArv>,
)

/** Vedtakets egen verdi ([arvetFraVedtak] er null), eller en verdi arvet fra vedtaket i [arvetFraVedtak]. */
data class VerdiMedKilde<T : Any>(
    val verdi: T,
    val arvetFraVedtak: InfotrygdKildevedtak?,
)

/**
 * Samme felt som [InfotrygdVedtakResponse], men [evaluering] og [gh] har kilde. De er null når verken vedtaket
 * eller et tidligere vedtak har en verdi. Inneholder helseopplysninger (GDPR art. 9).
 */
data class InfotrygdVedtakMedArv(
    val vedtakId: Long,
    val tknr: String,
    val saksblokk: String,
    val saksnr: Long,
    val typeSak: String,
    val kodeResultat: String,
    val datoInnvFom: LocalDate,
    /** Tom betyr at vedtaket er løpende. */
    val datoInnvTom: LocalDate?,
    val datoMottattSak: LocalDate,
    val kodeVedtaksnivaa: String,
    val typeBeregning: String,
    val tidspunktReg: LocalDateTime,
    val brukerid: String,
    val endringer: List<String>,
    val delytelser: List<InfotrygdDelytelseResponse>,
    /** Trygdetid og sats fra t_gh. Aldri en tom liste. */
    val gh: VerdiMedKilde<List<InfotrygdGhResponse>>?,
    val evaluering: VerdiMedKilde<InfotrygdEvaluering>?,
)

/**
 * Et vedtak uten egen verdi for gh, ev eller delytelse arver verdien fra vedtaket med nærmeste lavere vedtakId som har en egen verdi.
 * Evaluering og gh arves hver for seg og som hele objekter. Arv går ikke i kjede: kilden er alltid et vedtak
 * med egen verdi. Vedtak med ugyldig periode kan også være kilde. Se ADR-0012.
 */
object InfotrygdArv {
    fun fyllVedtakMedVerdierFraTidligereVedtak(stønader: List<InfotrygdStonadResponse>): List<InfotrygdStonadMedArv> {
        val alleVedtak = stønader.flatMap { it.vedtak }
        return stønader.map { stønad -> stønad.tilStønadMedArv(stønad.vedtak.map { it.fyllVedtakMedVerdierFraTidligereVedtak(alleVedtak) }) }
    }

    private fun InfotrygdVedtakResponse.fyllVedtakMedVerdierFraTidligereVedtak(alleVedtak: List<InfotrygdVedtakResponse>): InfotrygdVedtakMedArv =
        tilVedtakMedArv(
            gh = medKilde(alleVedtak) { it.gh.ifEmpty { null } },
            evaluering = medKilde(alleVedtak) { it.evaluering },
        )

    /**
     * Vedtakets egen verdi, ellers egen verdi fra vedtaket med nærmeste lavere vedtakId som har en.
     */
    private fun <T : Any> InfotrygdVedtakResponse.medKilde(
        alleVedtak: List<InfotrygdVedtakResponse>,
        egenVerdi: (InfotrygdVedtakResponse) -> T?,
    ): VerdiMedKilde<T>? =
        egenVerdi(this)?.let { VerdiMedKilde(verdi = it, arvetFraVedtak = null) }
            ?: alleVedtak
                .filter { it.vedtakId < vedtakId }
                .sortedByDescending { it.vedtakId }
                .firstNotNullOfOrNull { kilde -> egenVerdi(kilde)?.let { VerdiMedKilde(verdi = it, arvetFraVedtak = kilde.tilKildevedtak()) } }

    private fun InfotrygdVedtakResponse.tilKildevedtak() = InfotrygdKildevedtak(vedtakId = vedtakId, vedtakstidspunkt = tidspunktReg)

    private fun InfotrygdStonadResponse.tilStønadMedArv(vedtak: List<InfotrygdVedtakMedArv>) =
        InfotrygdStonadMedArv(
            stonadId = stonadId,
            personnr = personnr,
            kodeRutine = kodeRutine,
            datoStart = datoStart,
            kodeOpphor = kodeOpphor,
            datoOpphor = datoOpphor,
            oppdragId = oppdragId,
            tidspunktOpphort = tidspunktOpphort,
            tidspunktReg = tidspunktReg,
            brukerid = brukerid,
            vedtak = vedtak,
        )

    private fun InfotrygdVedtakResponse.tilVedtakMedArv(
        gh: VerdiMedKilde<List<InfotrygdGhResponse>>?,
        evaluering: VerdiMedKilde<InfotrygdEvaluering>?,
    ) = InfotrygdVedtakMedArv(
        vedtakId = vedtakId,
        tknr = tknr,
        saksblokk = saksblokk,
        saksnr = saksnr,
        typeSak = typeSak,
        kodeResultat = kodeResultat,
        datoInnvFom = datoInnvFom,
        datoInnvTom = datoInnvTom,
        datoMottattSak = datoMottattSak,
        kodeVedtaksnivaa = kodeVedtaksnivaa,
        typeBeregning = typeBeregning,
        tidspunktReg = tidspunktReg,
        brukerid = brukerid,
        endringer = endringer,
        delytelser = delytelser,
        gh = gh,
        evaluering = evaluering,
    )
}
