package no.nav.gjenlevende.bs.sak.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
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
@Tilgangskontroll
@RequestMapping(path = ["/api/brev"])
@Tag(name = "BrevController", description = "Endepunkter for håndtering av brev")
class BrevController(
    private val brevService: BrevService,
    private val taskService: TaskService,
) {
    @PostMapping("/lag-task/{behandlingId}") // TODO renames til beslutte-behandling elr noe sånt
    @PreAuthorize("hasRole('ATTESTERING')")
    @Operation(
        summary = "Oppretter brev-task",
        description = "Lager task som genererer pdf-brev",
    )
    fun lagBrevTask(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<String> {
        brevService.oppdaterBeslutterForBrev(behandlingId)
        val task = brevService.lagBrevPdfTask(behandlingId)
        taskService.save(task)

        return ResponseEntity.ok("OK")
    }

    @PostMapping("/mellomlagre/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER')")
    @Operation(
        summary = "Mellomlagrer brev",
        description = "Mellomlagrer brevJson for gitt behandlingId",
    )
    fun mellomlagreBrev(
        @PathVariable behandlingId: UUID,
        @RequestBody brevRequest: BrevRequest,
    ): ResponseEntity<Void> {
        brevService.mellomlagreBrev(behandlingId, brevRequest)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/hentMellomlagretBrev/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('ATTESTERING')")
    @Operation(
        summary = "Henter mellomlagret brev",
        description = "Returnerer brevJson for gitt behandlingId",
    )
    fun hentMellomlagretBrev(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<BrevRequest> {
        val brev = brevService.hentBrev(behandlingId) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(brev.brevJson)
    }
}
