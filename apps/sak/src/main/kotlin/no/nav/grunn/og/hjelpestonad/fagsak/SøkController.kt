package no.nav.grunn.og.hjelpestonad.fagsak

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class SøkeresultatResponse(
    val navn: String,
    val fødselsdato: LocalDate?,
    val personident: String,
    val fagsakPersonId: UUID?,
    val harFagsak: Boolean,
)

// Personident i body, ikke i URL-en, så fødselsnummeret ikke havner i URL-logger.
data class SøkRequest(
    val personident: String,
)

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
@RequestMapping(path = ["/api/sok"])
class SøkController(
    private val søkService: SøkService,
) {
    @PostMapping("/person")
    fun søkPerson(
        @RequestBody søkRequest: SøkRequest,
    ): SøkeresultatResponse {
        validerErPersonident(søkRequest.personident)
        return søkService.søkPerson(søkRequest.personident)
    }

    @GetMapping("/fagsak-person/{fagsakPersonId}")
    fun søkPersonMedFagsakPersonId(
        @PathVariable("fagsakPersonId") fagsakPersonId: UUID,
    ): SøkeresultatResponse = søkService.søkMedFagsakPersonId(fagsakPersonId)
}
