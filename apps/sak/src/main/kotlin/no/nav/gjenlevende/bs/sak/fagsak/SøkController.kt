package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

data class Søkeresultat(
    val navn: String,
    val fødselsdato: LocalDate?,
    val personident: String,
    val fagsakPersonId: UUID?,
    val harFagsak: Boolean,
)

data class SøkRequest(
    val personident: String?,
    val fagsakPersonId: UUID?,
)

const val LENGDE_PERSONIDENT = 11

@RestController
@PreAuthorize("hasRole('SAKSBEHANDLER')")
@RequestMapping(path = ["/api/sok"])
class SøkController(
    private val søkService: SøkService,
) {
    @PostMapping("/person")
    fun søkPerson(
        @RequestBody søkRequest: SøkRequest,
    ): Søkeresultat {
        val personident = søkRequest.personident
        when {
            personident != null -> {
                validerErPersonident(personident)
                return søkService.søkPerson(personident)
            }

            søkRequest.fagsakPersonId != null -> {
                return søkService.søkMedFagsakPersonId(søkRequest.fagsakPersonId)
            }

            else -> {
                throw Feil(
                    melding = "Må oppgi enten personident eller fagsakPersonId",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
    }

    internal fun validerErPersonident(personident: String) {
        if (personident.isBlank()) {
            throw Feil(
                melding = "Personident kan ikke være tom",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        if (personident.length != LENGDE_PERSONIDENT) {
            throw Feil(
                melding = "Personident må være $LENGDE_PERSONIDENT siffer",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        if (!personident.all { it.isDigit() }) {
            throw Feil(
                melding = "Personident kan kun inneholde tall",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
