package no.nav.grunn.og.hjelpestonad.infotrygd

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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

    @PostMapping("/stonader")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @Operation(
        summary = "Hent stønader for person fra Infotrygd",
        description = "Henter stønadene for én stønadstype som et tre fra t_stonad til vedtak, endringer, delytelser, trygdetid og sats og evaluering, med ICD-10-tekst for diagnosene. Inneholder helseopplysninger.",
        security = [SecurityRequirement(name = "oauth2")],
    )
    fun hentStønaderForPerson(
        @RequestBody request: InfotrygdStonaderRequest,
    ): ResponseEntity<InfotrygdStonaderResponse> {
        PersonidentValidator.validerPersonident(request.personident)
        tilgangService.validerTilgangTilPerson(request.personident)

        val response =
            infotrygdClient
                .hentStønader(personident = request.personident, stønadstype = request.stønadstype)
                .tilInfotrygdStonaderResponse()

        logger.info("Hentet {} stønader fra Infotrygd, stønadstype={}", response.stonader.size, request.stønadstype)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(response)
    }
}
