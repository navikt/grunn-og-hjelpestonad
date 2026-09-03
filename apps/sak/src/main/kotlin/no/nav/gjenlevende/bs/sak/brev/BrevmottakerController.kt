package no.nav.gjenlevende.bs.sak.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmottakerRequest
import no.nav.gjenlevende.bs.sak.brev.dto.BrevmottakerDto
import no.nav.gjenlevende.bs.sak.brev.dto.tilDto
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
@RequestMapping(path = ["/api/brevmottaker"])
@Tag(name = "BrevmottakerController", description = "Endepunkter for håndtering av brevmottakere")
class BrevmottakerController(
    private val brevmottakerService: BrevmottakerService,
) {
    @GetMapping("/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER')")
    @Operation(
        summary = "Henter brevmottakere",
        description = "Henter alle brevmottakere for gitt behandlingId",
    )
    fun hentBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<List<BrevmottakerDto>> {
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId).map { it.tilDto() }
        return ResponseEntity.ok(brevmottakere)
    }

    @PostMapping("/settMottakere/{behandlingId}")
    @PreAuthorize("hasRole('SAKSBEHANDLER')")
    @Operation(
        summary = "Oppdaterer brevmottakere",
        description = "Oppdaterer brevmottakere for gitt behandlingId",
    )
    fun oppdaterBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakere: List<BrevmottakerRequest>,
    ): ResponseEntity<String> {
        brevmottakerService.oppdaterBrevmottakere(
            behandlingId,
            brevmottakere.map { it.tilBrevmottaker(behandlingId) },
        )
        return ResponseEntity.ok("OK")
    }
}
