package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InfotrygdService(
    private val infotrygdClient: InfotrygdClient,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdService::class.java)

    fun hentStønader(
        personident: String,
        stønadstype: StonaderRequest.Stønadstype,
    ): InfotrygdStonaderResponse =
        InfotrygdArv
            .fyllVedtakMedVerdierFraTidligereVedtak(infotrygdClient.hentStønader(personident = personident, stønadstype = stønadstype).stonader)
            .tilInfotrygdStonaderResponse()

    fun hentGjeldendePerioder(
        personident: String,
        stønadstype: StonaderRequest.Stønadstype,
    ): InfotrygdGjeldendePerioderResponse {
        val stønader = InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak(infotrygdClient.hentStønader(personident = personident, stønadstype = stønadstype).stonader)

        val utenforRekkefølge = GjeldendeInfotrygdPerioder.antallVedtakUtenforRekkefølge(stønader)
        if (utenforRekkefølge > 0) {
            logger.warn(
                "{} vedtak fra Infotrygd er registrert før vedtaket med nærmeste lavere vedtakId, stønadstype={}",
                utenforRekkefølge,
                stønadstype,
            )
        }

        val perioder = GjeldendeInfotrygdPerioder.utled(stønader)
        logger.info("Utledet {} gjeldende perioder fra Infotrygd, stønadstype={}", perioder.size, stønadstype)
        return perioder.tilInfotrygdGjeldendePerioderResponse()
    }
}
