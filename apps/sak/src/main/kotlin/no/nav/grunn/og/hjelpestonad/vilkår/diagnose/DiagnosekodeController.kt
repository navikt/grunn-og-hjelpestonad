package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping("/api/diagnosekoder")
class DiagnosekodeController {
    /**
     * POST og ikke GET: søketeksten kan være en diagnose, som er helseopplysning etter GDPR
     * artikkel 9. I en URL ville den havnet i tracing og logger.
     */
    @PostMapping("/sok")
    fun søkDiagnosekoder(
        @RequestBody request: DiagnosekodeSøkRequest,
    ): ResponseEntity<List<DiagnosekodeResponse>> {
        val treff = Icd10.søk(request.søketekst)
        return ResponseEntity.ok(treff.map { it.tilResponse() })
    }
}

data class DiagnosekodeSøkRequest(
    val søketekst: String,
)

data class DiagnosekodeResponse(
    val kode: String,
    val tekst: String,
)

fun Diagnosekode.tilResponse() = DiagnosekodeResponse(kode = kode, tekst = tekst)
