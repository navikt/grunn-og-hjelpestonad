package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.EvalueringService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MigreringskandidatService(
    private val stønadRepository: StønadRepository,
    private val evalueringService: EvalueringService,
) {
    private val logger = LoggerFactory.getLogger(MigreringskandidatService::class.java)

    /** Se [StønadRepository.hentKandidaterMedFørstegangsvedtak] og [finnPersonerMedHjemmelbokstav]. */
    fun finnPersonerMedFørstegangsvedtak(
        satsGrunnstønad: Long,
        hjemmelbokstav: String,
        antall: Int,
    ): Set<String> {
        val startKandidater = System.nanoTime()
        val kandidater = stønadRepository.hentKandidaterMedFørstegangsvedtak(satsGrunnstønad)
        logger.info("Hentet {} kandidater med førstegangsvedtak på {} ms", kandidater.size, millisekunderSiden(startKandidater))

        val startEvaluering = System.nanoTime()
        var antallBiter = 0
        val personerMedTreff =
            finnPersonerMedHjemmelbokstav(
                kandidater = kandidater,
                hjemmelbokstav = hjemmelbokstav,
                antall = antall,
                bitstørrelse = BITSTØRRELSE,
                hentEvalueringer = { vedtakIder ->
                    antallBiter++
                    evalueringService.hentEvalueringerForVedtak(vedtakIder)
                },
            )
        logger.info(
            "Fant {} av {} personer etter {} biter med evaluering på {} ms",
            personerMedTreff.size,
            antall,
            antallBiter,
            millisekunderSiden(startEvaluering),
        )
        return personerMedTreff
    }

    private fun millisekunderSiden(startNanosekunder: Long): Long = (System.nanoTime() - startNanosekunder) / 1_000_000

    companion object {
        const val BITSTØRRELSE = 1000
    }
}
