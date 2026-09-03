package no.nav.gjenlevende.bs.sak.behandling.årsak

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class ÅrsakBehandlingRequest(
    val kravdato: LocalDate,
    val årsak: Årsak,
    val beskrivelse: String = "",
)

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/arsak")
class ÅrsakBehandlingController(
    private val årsakBehandlingService: ÅrsakBehandlingService,
) {
    @GetMapping("/{behandlingId}")
    fun hentÅrsakBehandling(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<ÅrsakBehandlingDto> {
        val årsak = årsakBehandlingService.hentÅrsakBehandling(behandlingId)

        return if (årsak == null) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.ok(årsak.tilDto())
        }
    }

    @PostMapping("/{behandlingId}")
    fun lagreÅrsakBehandling(
        @PathVariable behandlingId: UUID,
        @RequestBody årsakBehandlingRequest: ÅrsakBehandlingRequest,
    ): ResponseEntity<ÅrsakBehandlingDto> {
        val årsak = årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, årsakBehandlingRequest)
        return ResponseEntity.ok(årsak.tilDto())
    }
}
