package no.nav.grunn.og.hjelpestonad.infotrygd

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PerioderResponse
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import no.nav.grunn.og.hjelpestonad.util.PersonidentValidator
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@Tag(
    name = "Infotrygd",
    description = "Oppslag mot grunn-og-hjelpestonad-infotrygd",
)
class InfotrygdController(
    private val infotrygdClient: InfotrygdClient,
    private val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdController::class.java)

    @PostMapping("/perioder")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @Operation(
        summary = "Hent vedtaksperioder for person fra Infotrygd",
        description = "Henter alle vedtaksperioder for en gitt person basert på personident.",
        security = [SecurityRequirement(name = "oauth2")],
    )
    fun hentPerioderForPerson(
        @RequestBody request: PersonidentRequest,
    ): ResponseEntity<PerioderResponse> {
        logger.info("Henter perioder for person fra grunn-og-hjelpestonad-infotrygd")

        PersonidentValidator.validerPersonident(request.personident)
        tilgangService.validerTilgangTilPerson(request.personident)

        val response =
            infotrygdClient.hentPerioderForPerson(
                personident = request.personident,
            )

        logger.info("Hentet perioder fra Infotrygd: ${response.grunnstønad.size} grunnstønad, ${response.hjelpestønad.size} hjelpestønad")
        return ResponseEntity.ok(response)
    }

    @PostMapping("/diagnose")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @Operation(
        summary = "Hent diagnoser for person fra Infotrygd",
        description = "Henter diagnosene registrert i siste måned per stønadstype. Tom liste med stønadstyper gir begge.",
        security = [SecurityRequirement(name = "oauth2")],
    )
    fun hentDiagnoserForPerson(
        @RequestBody request: InfotrygdDiagnoseRequest,
    ): ResponseEntity<InfotrygdDiagnoserResponse> {
        PersonidentValidator.validerPersonident(request.personident)
        tilgangService.validerTilgangTilPerson(request.personident)
        // Diagnosekoder er helseopplysninger (GDPR art. 9). Feildetaljer logges bare i Team Logs.
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(
                infotrygdClient
                    .hentDiagnoser(
                        personident = request.personident,
                        stønadstyper = request.stønadstyper,
                    ).tilInfotrygdDiagnoserResponse(),
            )
    }
}
