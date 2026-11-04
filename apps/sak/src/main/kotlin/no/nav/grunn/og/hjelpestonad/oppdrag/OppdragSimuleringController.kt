package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Synkron simulering mot oppdrag. Brukes foreløpig for å teste integrasjonen fra simuleringsfanen. */
@RestController
@RequestMapping(path = ["/api/oppdrag/simulering"])
@PreAuthorize("hasRole('SAKSBEHANDLER')")
class OppdragSimuleringController(
    private val tilgangService: TilgangService,
    private val oppdragSimuleringService: OppdragSimuleringService,
) {
    @PostMapping("/{behandlingId}")
    fun simulerMotOppdrag(
        @PathVariable behandlingId: UUID,
    ): OppdragSimuleringResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragSimuleringService.simuler(behandlingId)
    }
}
