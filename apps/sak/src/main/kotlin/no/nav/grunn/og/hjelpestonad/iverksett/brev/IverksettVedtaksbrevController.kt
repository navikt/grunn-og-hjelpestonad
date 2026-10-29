package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.spring.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/iverksett"])
@Tag(name = "iverksettVedtaksbrevController", description = "Endepunkt for utvikling")
class IverksettVedtaksbrevController(
    private val taskService: TaskService,
    private val iverksettVedtaksbrevService: IverksettVedtaksbrevService,
    private val distribuerVedtaksbrevTask: DistribuerVedtaksbrevTask,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/lag-iverksettVedtaksbrevController-task/{behandlingId}")
    @PreAuthorize("hasRole('BESLUTTER')")
    @Operation(
        summary = "Oppretter brev-task",
        description = "Lager task som genererer pdf-brev",
    )
    fun lagIverksettVedtaksbrevTask(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<String> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        taskService.opprettIEgenTransaksjon(
            JournalførVedtaksbrevTask.TYPE,
            iverksettVedtaksbrevService.opprettIverksettVedtaksbrevTask(behandlingId),
        )

        return ResponseEntity.ok("OK")
    }

    @PostMapping("/distribuer-vedtaksbrev/{behandlingId}")
    @PreAuthorize("hasRole('BESLUTTER')")
    @Operation(
        summary = "Distribuerer vedtaksbrev",
        description = "Testendepunkt for å distribuere vedtaksbrev basert på behandlingId",
    )
    fun distribuerVedtaksbrev(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<String> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        taskService.opprettIEgenTransaksjon(
            DistribuerVedtaksbrevTask.TYPE,
            iverksettVedtaksbrevService.opprettDistribuerVedtaksbrevTask(behandlingId),
        )

        return ResponseEntity.ok("OK")
    }
}
