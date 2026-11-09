package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Midlertidig endepunkt for å teste iverksetting mot oppdrag i dev. Finnes bare når
 * `oppdrag.iverksetting.testendepunkt-aktivert=true`, som ikke er satt i prod.
 */
@RestController
@RequestMapping(path = ["/api/oppdrag/iverksetting"])
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@ConditionalOnProperty(name = ["oppdrag.iverksetting.testendepunkt-aktivert"], havingValue = "true")
class OppdragIverksettingController(
    private val tilgangService: TilgangService,
    private val oppdragIverksettingService: OppdragIverksettingService,
) {
    @PostMapping("/{behandlingId}")
    fun iverksettMotOppdrag(
        @PathVariable behandlingId: UUID,
    ): OppdragIverksettingResponse {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragIverksettingService.iverksett(behandlingId)
    }

    @GetMapping("/{behandlingId}/status")
    fun hentOppdragStatus(
        @PathVariable behandlingId: UUID,
    ): OppdragStatusSvar {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return oppdragIverksettingService.hentStatus(behandlingId)
    }
}
