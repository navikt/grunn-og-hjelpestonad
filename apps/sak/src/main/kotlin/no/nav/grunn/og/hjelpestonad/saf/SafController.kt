package no.nav.grunn.og.hjelpestonad.saf

import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/saf")
class SafController(
    private val safService: SafService,
    private val tilgangService: TilgangService,
) {
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @PostMapping("/dokumenter")
    fun hentJournalPostForBrukerId(
        @RequestBody request: HentDokumenterRequest,
    ): ResponseEntity<List<DokumentinfoResponse>> {
        tilgangService.validerTilgangTilFagsakPerson(request.fagsakPersonId)
        val data =
            safService.finnDokumenterForPerson(request.fagsakPersonId)

        return ResponseEntity.ok(data)
    }

    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER')")
    @GetMapping(
        path = ["/{journalpostId}/dokument-pdf/{dokumentInfoId}", "/{journalpostId}/dokument-pdf/{dokumentInfoId}/{filnavn}"],
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun hentDokumentSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
        @RequestParam fagsakPersonId: UUID,
    ): ByteArray {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId)
        return safService.hentDokumentForPerson(
            fagsakPersonId = fagsakPersonId,
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
        )
    }
}
