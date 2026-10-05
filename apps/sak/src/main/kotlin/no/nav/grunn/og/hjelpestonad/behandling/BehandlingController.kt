package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.behandling.henleggBehandling.HenleggBehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling.OpprettBehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class OpprettRequest(
    val fagsakId: UUID,
)

data class HentRequest(
    val behandlingId: UUID,
)

data class HentBehandlingerRequest(
    val fagsakId: UUID,
)

data class HenleggRequest(
    val behandlingId: UUID,
)

data class OpprettBehandlingResponse(
    val behandlingId: UUID,
)

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping(path = ["/api/behandling"])
class BehandlingController(
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val henleggBehandlingService: HenleggBehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
) {
    @PostMapping("/opprett")
    fun opprettBehandling(
        @RequestBody opprettRequest: OpprettRequest,
    ): ResponseEntity<OpprettBehandlingResponse> {
        val behandling = opprettBehandlingService.opprettBehandling(opprettRequest.fagsakId)

        return ResponseEntity.ok(OpprettBehandlingResponse(behandlingId = behandling.id))
    }

    @PostMapping("/henlegg")
    fun henleggBehandling(
        @RequestBody henleggRequest: HenleggRequest,
    ): ResponseEntity<Void> {
        henleggBehandlingService.henleggBehandling(behandlingId = henleggRequest.behandlingId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/hentBehandlinger")
    fun hentBehandlinger(
        @RequestBody hentBehandlingerRequest: HentBehandlingerRequest,
    ): ResponseEntity<List<BehandlingResponse>> {
        val fagsakId = hentBehandlingerRequest.fagsakId

        val behandlinger = behandlingService.hentBehandlingerFraFagsak(fagsakId)
        return ResponseEntity.ok(
            behandlinger?.map {
                val sisteEndring = endringshistorikkService.hentSisteEndring(it.id)
                it.tilResponse(sisteEndring)
            },
        )
    }

    @PostMapping("/hent")
    fun hentBehandling(
        @RequestBody hentRequest: HentRequest,
    ): ResponseEntity<BehandlingResponse> {
        val behandlingId = hentRequest.behandlingId

        val behandling = behandlingService.hentBehandling(behandlingId)
        val sisteEndring = behandling?.let { endringshistorikkService.hentSisteEndring(it.id) }
        return ResponseEntity.ok(behandling?.tilResponse(sisteEndring))
    }
}
