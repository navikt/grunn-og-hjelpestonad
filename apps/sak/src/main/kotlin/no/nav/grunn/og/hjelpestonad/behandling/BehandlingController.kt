package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
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
    private val endringshistorikkService: EndringshistorikkService,
) {
    @PostMapping("/opprett")
    fun opprettBehandling(
        @RequestBody opprettRequest: OpprettRequest,
    ): ResponseEntity<OpprettBehandlingResponse> {
        val fagsakId = opprettRequest.fagsakId

        if (behandlingService.finnesÅpenBehandling(opprettRequest.fagsakId)) {
            throw Feil("Finnes åpen behandling")
        }

        val behandling = behandlingService.opprettBehandling(fagsakId)

        return ResponseEntity.ok(OpprettBehandlingResponse(behandlingId = behandling.id))
    }

    @PostMapping("/henlegg")
    fun henleggBehandling(
        @RequestBody henleggRequest: HenleggRequest,
    ): ResponseEntity<Void> {
        behandlingService.henleggBehandling(behandlingId = henleggRequest.behandlingId)
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
