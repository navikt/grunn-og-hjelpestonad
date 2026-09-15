package no.nav.grunn.og.hjelpestonad.beslutter

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.beslutter.dto.TotrinnskontrollStatusResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping(path = ["/api/beslutter"])
@Tag(name = "BeslutterController", description = "Endepunkter for beslutter-funksjonalitet")
class BeslutterController(
    private val beslutterService: BeslutterService,
    private val totrinnskontrollService: TotrinnskontrollService,
) {
    @PostMapping("/send-til-beslutter/{behandlingId}")
    fun sendTilBeslutter(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<Map<String, String>> {
        beslutterService.sendTilBeslutter(behandlingId)
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }

    @PostMapping("/angre-send-til-beslutter/{behandlingId}")
    fun angreSendTilBeslutter(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<Map<String, String>> {
        beslutterService.angreSendTilBeslutter(behandlingId)
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }

    @PostMapping("/beslutt-vedtak/{behandlingId}")
    fun besluttVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody beslutteVedtakRequest: BeslutteVedtakRequest,
    ): ResponseEntity<Map<String, String>> {
        beslutterService.besluttVedtak(
            behandlingId = behandlingId,
            beslutteVedtakRequest = beslutteVedtakRequest,
        )
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }

    @GetMapping("/totrinnskontroll-status/{behandlingId}")
    fun hentTotrinnskontrollStatus(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<TotrinnskontrollStatusResponse> {
        val status = totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
        return ResponseEntity.ok(status)
    }
}
