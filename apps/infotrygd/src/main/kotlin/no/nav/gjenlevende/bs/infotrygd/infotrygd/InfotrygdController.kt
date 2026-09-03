package no.nav.gjenlevende.bs.infotrygd.infotrygd

import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.VedtakPeriodeRequest
import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.VedtakPeriodeResponse
import no.nav.gjenlevende.bs.infotrygd.util.PersonidentValidator
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
open class InfotrygdController(
    private val infotrygdService: InfotrygdService,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdController::class.java)

    @PostMapping("/perioder")
    @PreAuthorize("hasRole('SAKSBEHANDLER') and hasRole('ATTESTERING') and hasRole('LES')")
    fun hentPerioderForPerson(
        @RequestBody request: VedtakPeriodeRequest,
    ): ResponseEntity<VedtakPeriodeResponse> =
        try {
            PersonidentValidator.validerPersonident(request.personident)
            val perioder = infotrygdService.hentVedtakPerioder(request.personident)
            ResponseEntity.ok(perioder)
        } catch (e: Exception) {
            logger.error("Feil ved henting av perioder fra Infotrygd: ${e.message}", e)
            throw e
        }
}
