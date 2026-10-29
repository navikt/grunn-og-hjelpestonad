package no.nav.grunn.og.hjelpestonad.infotrygd

import io.swagger.v3.oas.annotations.Operation
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
    private val infotrygdService: InfotrygdService,
    private val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(InfotrygdController::class.java)

    @PostMapping("/stonader")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @Operation(
        summary = "Hent stønader for person fra Infotrygd",
        description = "Henter stønadene for én stønadstype som et tre fra t_stonad til vedtak, endringer, delytelser, trygdetid og sats og evaluering, med ICD-10-tekst for diagnosene. Inneholder helseopplysninger.",
    )
    fun hentStønaderForPerson(
        @RequestBody request: InfotrygdStonaderRequest,
    ): ResponseEntity<InfotrygdStonaderResponse> {
        PersonidentValidator.validerPersonident(request.personident)
        tilgangService.validerTilgangTilPerson(request.personident)

        val response =
            infotrygdService.hentStønader(personident = request.personident, stønadstype = request.stønadstype)

        logger.info("Hentet {} stønader fra Infotrygd, stønadstype={}", response.stonader.size, request.stønadstype)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(response)
    }

    @PostMapping("/gjeldende-perioder")
    @PreAuthorize("hasRole('SAKSBEHANDLER') or hasRole('BESLUTTER') or hasRole('LESETILGANG')")
    @Operation(
        summary = "Hent gjeldende perioder for person fra Infotrygd",
        description = "Hvert vedtak blir én periode fra datoInnvFom til datoInnvTom. Nyere vedtakId overskriver eldre vedtak i hele sin periode, også på tvers av stønadene. Et vedtak med tom før eller lik fom gir 400 med melding om vedtaket. Vedtakene har arvet evaluering og gh der de mangler egen verdi. Inneholder helseopplysninger.",
    )
    fun hentGjeldendePerioderForPerson(
        @RequestBody request: InfotrygdStonaderRequest,
    ): ResponseEntity<InfotrygdGjeldendePerioderResponse> {
        PersonidentValidator.validerPersonident(request.personident)
        tilgangService.validerTilgangTilPerson(request.personident)

        val response =
            infotrygdService.hentGjeldendePerioder(personident = request.personident, stønadstype = request.stønadstype)

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(response)
    }
}
