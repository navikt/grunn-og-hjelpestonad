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

    /** Se [finnPersonerMedHjemmelbokstav] og [StønadRepository.hentPartiMedFørstegangsvedtak]. */
    fun finnPersonerMedFørstegangsvedtak(
        satsGrunnstønad: Long,
        hjemmelbokstav: String,
        antall: Int,
    ): Set<String> {
        var antallPartier = 0
        val personerMedTreff =
            finnPersonerMedHjemmelbokstav(
                hjemmelbokstav = hjemmelbokstav,
                antall = antall,
                hentParti = { førStønadId ->
                    antallPartier++
                    stønadRepository.hentPartiMedFørstegangsvedtak(satsGrunnstønad, førStønadId, PARTISTØRRELSE)
                },
                hentEvalueringer = evalueringService::hentEvalueringerForVedtak,
            )
        logger.info(
            "Fant {} av {} personer med førstegangsvedtak etter {} partier",
            personerMedTreff.size,
            antall,
            antallPartier,
        )
        return personerMedTreff
    }

    companion object {
        const val PARTISTØRRELSE = 1000
    }
}
