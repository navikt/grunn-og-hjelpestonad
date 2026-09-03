package no.nav.gjenlevende.bs.sak.oppgave

import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
import no.nav.gjenlevende.bs.sak.oppgave.dto.AnsvarligSaksbehandlerDto
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class HentAnsvarligSaksbehandlerRequest(
    val behandlingId: UUID,
)

data class FordelOppgaveRequest(
    val behandlingId: UUID,
    val saksbehandler: String,
)

@RestController
@Tilgangskontroll
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping(path = ["/api/oppgave"])
class OppgaveController(
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val oppgaveService: OppgaveService,
) {
    @PostMapping("/ansvarlig-saksbehandler")
    fun hentAnsvarligSaksbehandler(
        @RequestBody request: HentAnsvarligSaksbehandlerRequest,
    ): ResponseEntity<AnsvarligSaksbehandlerDto> {
        val ansvarligSaksbehandler = ansvarligSaksbehandlerService.hentAnsvarligSaksbehandler(request.behandlingId)
        return ResponseEntity.ok(ansvarligSaksbehandler)
    }

    @PostMapping("/fordel")
    fun fordelOppgave(
        @RequestBody request: FordelOppgaveRequest,
    ): ResponseEntity<Long> {
        val oppgaveId =
            oppgaveService.fordelOppgave(
                behandlingId = request.behandlingId,
                saksbehandler = request.saksbehandler,
            )
        return ResponseEntity.ok(oppgaveId)
    }
}
