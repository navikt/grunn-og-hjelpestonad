package no.nav.gjenlevende.bs.sak.saf

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saf")
class SafController(
    private val safService: SafService,
) {
    @PostMapping("/dokumenter")
    fun hentJournalPostForBrukerId(
        @RequestBody request: HentDokumenterRequest,
    ): ResponseEntity<List<DokumentinfoDto>> {
        val data =
            safService.finnDokumenterForPerson(request.fagsakPersonId)

        return ResponseEntity.ok(data)
    }

    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('ATTESTERING')")
    @GetMapping(
        path = ["/{journalpostId}/dokument-pdf/{dokumentInfoId}", "/{journalpostId}/dokument-pdf/{dokumentInfoId}/{filnavn}"],
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun hentDokumentSomPdf(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): ByteArray = safService.hentDokument(journalpostId, dokumentInfoId)
}
