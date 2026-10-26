package no.nav.grunn.og.hjelpestonad.infotrygd.evaluering

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EvalueringService(
    private val evalueringRepository: EvalueringRepository,
) {
    private val logger = LoggerFactory.getLogger(EvalueringService::class.java)

    fun hentEvalueringerForVedtak(vedtakIder: Set<Long>): Map<Long, InfotrygdEvaluering> {
        val rader = evalueringRepository.hentEvalueringsraderForVedtak(vedtakIder)

        logUgyldigVirkningstidspunkter(rader)

        val evalueringer = tilEvalueringer(rader)
        logger.info(
            "Hentet evaluering for {} av {} vedtak, {} med motstridende felt, per posisjon: {}",
            evalueringer.size,
            vedtakIder.size,
            evalueringer.values.count { it.motstridendeFelt.isNotEmpty() },
            Ev10Posisjon.entries.associateWith { posisjon ->
                evalueringer.values.count { posisjon in it.motstridendeFelt }
            },
        )
        return evalueringer
    }

    private fun logUgyldigVirkningstidspunkter(rader: List<Ev10Rad>) {
        rader
            .filter {
                it.posisjon == Ev10Posisjon.VIRKNINGSTIDSPUNKT.kode &&
                    lesVirkningstidspunkt(it.tekst) == LestVirkningstidspunkt.Ugyldig
            }.forEach { rad ->
                secureLogger.warn(
                    "Ugyldig virkningstidspunkt i ev_data_10. personident={} vedtakId={} måned={} ev10_text={}",
                    rad.personident,
                    rad.vedtakId,
                    rad.månedNotertIInfotrygd,
                    rad.tekst,
                )
                logger.warn("Ugyldig virkningstidspunkt i posisjon 165 i ev_data_10. Verdien er logget i team logs.")
            }
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
