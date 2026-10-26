package no.nav.grunn.og.hjelpestonad.infotrygd.stønader

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.EvalueringService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class StønaderService(
    private val stønaderRepository: StønaderRepository,
    private val evalueringService: EvalueringService,
) {
    /**
     * Bygger treet stønad → vedtak → endringer, delytelser, gh og evaluering.
     *
     * REPEATABLE_READ gir alle spørringene samme øyeblikksbilde. Med READ COMMITTED kan replikeringen skrive rader
     * mellom spørringene, slik at f.eks. vedtakene og delytelsene kommer fra ulike tidspunkt. Replikaen er
     * likevel ikke konsistent på tvers av tabeller mens replikeringen pågår, så et vedtak kan mangle delytelser.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun hentStønader(request: StonaderRequest): StonaderResponse {
        val stønader = stønaderRepository.hentStønader(request.personidenter, request.stønadstype)
        val vedtakPerStønad = stønaderRepository.hentVedtak(stønader.map { it.stonadId }.toSet(), request.personidenter)
        // Barnetabellene og evalueringen arver eierskapet fra vedtaket. Hent alltid vedtakIder fra hentVedtak,
        // som bare gir vedtak på personene i forespørselen.
        val vedtakIder =
            vedtakPerStønad.values
                .flatten()
                .map { it.vedtakId }
                .toSet()

        val endringer = stønaderRepository.hentEndringer(vedtakIder)
        val delytelser = stønaderRepository.hentDelytelser(vedtakIder)
        val gh = stønaderRepository.hentGh(vedtakIder)
        val evalueringer = evalueringService.hentEvalueringerForVedtak(vedtakIder)

        return StonaderResponse(
            stonader =
                stønader.map { stønad ->
                    stønad.copy(
                        vedtak =
                            vedtakPerStønad[stønad.stonadId].orEmpty().map { vedtak ->
                                vedtak.copy(
                                    endringer = endringer[vedtak.vedtakId].orEmpty(),
                                    delytelser = delytelser[vedtak.vedtakId].orEmpty(),
                                    gh = gh[vedtak.vedtakId].orEmpty(),
                                    evaluering = evalueringer[vedtak.vedtakId],
                                )
                            },
                    )
                },
        )
    }
}
