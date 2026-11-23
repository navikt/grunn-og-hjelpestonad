package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringResponse
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Simulering mot Oppdrag (ADR-0018).
 *
 * Rollene står på hver metode, ikke på klassen. Hver Azure-gruppe gir bare én rolle, så en beslutter har ikke
 * `SAKSBEHANDLER`. Alle med lesetilgang kan se den lagrede simuleringen. Bare saksbehandler kan simulere på nytt, og
 * servicen krever i tillegg at hen er ansvarlig saksbehandler og at behandlingen kan endres.
 */
@RestController
@RequestMapping(path = ["/api/oppdrag/simulering"])
class OppdragSimuleringController(
    private val tilgangService: TilgangService,
    private val oppdragSimuleringService: OppdragSimuleringService,
) {
    @PostMapping("/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER')")
    fun simulerMotOppdrag(
        @PathVariable behandlingId: UUID,
    ): SimuleringResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragSimuleringService.simulerPåNytt(behandlingId)
    }

    /** Den lagrede simuleringen. Kaller ikke Oppdrag. */
    @GetMapping("/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    fun hentSimulering(
        @PathVariable behandlingId: UUID,
    ): SimuleringResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragSimuleringService.hentSimulering(behandlingId)
    }
}
