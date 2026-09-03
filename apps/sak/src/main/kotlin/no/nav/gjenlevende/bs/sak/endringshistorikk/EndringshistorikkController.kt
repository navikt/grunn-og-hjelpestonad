package no.nav.gjenlevende.bs.sak.endringshistorikk

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tilgangskontroll
@PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('ATTESTERING')")
@RequestMapping(path = ["/api/endringshistorikk"])
@Tag(name = "EndringshistorikkController", description = "Endepunkter for endringshistorikk")
class EndringshistorikkController(
    private val endringshistorikkService: EndringshistorikkService,
) {
    @GetMapping("/{behandlingId}")
    fun hentEndringshistorikk(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<BehandlingEndringDto>> {
        val historikk = endringshistorikkService.hentEndringshistorikk(behandlingId)
        return ResponseEntity.ok(historikk.map { it.tilDto() })
    }
}
