package no.nav.gjenlevende.bs.sak.tilgangskontroll

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/tilgangsmaskin"])
@Tag(name = "Tilgangsmaskin", description = "Endepunkter for testing av tilgangskontroll via Tilgangsmaskinen")
class TilgangsmaskinController(
    private val tilgangsmaskinClient: TilgangsmaskinClient,
) {
    private val logger = LoggerFactory.getLogger(TilgangsmaskinController::class.java)

    @Operation(
        summary = "Bulk-sjekk tilgang til flere brukere (forenklet)",
        description = "Sjekker kjerneregelsett (habilitet) og returnerer forenklet respons med harTilgang-flagg",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bulk sjekk fungerte",
                content = [Content(schema = Schema(implementation = ForenkletBulkTilgangsResponse::class))],
            ),
        ],
    )
    @PostMapping("/sjekk/bulk", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sjekkTilgangBulkForenklet(
        @RequestBody request: BulkTilgangsRequest,
    ): ForenkletBulkTilgangsResponse {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        logger.info("Bulk sjekker tilgang (kjerne) for saksbehandler $navIdent til ${request.personidenter.size} brukere")
        val respons =
            tilgangsmaskinClient.sjekkTilgangBulk(
                personidenter = request.personidenter,
                regelType = RegelType.KJERNE_REGELTYPE,
            )
        return tilForenkletRespons(respons)
    }

    @Operation(
        summary = "Bulk sjekk tilgang til flere brukere (komplett)",
        description = "Sjekker komplett regelsett inkludert geografiske begrensninger. Returnerer full respons med alle detaljer.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bulk sjekk fungerte",
                content = [Content(schema = Schema(implementation = BulkTilgangsResponse::class))],
            ),
        ],
    )
    @PostMapping("/sjekk/bulk/komplett", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sjekkTilgangBulkKomplett(
        @RequestBody request: BulkTilgangsRequest,
    ): BulkTilgangsResponse {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        logger.info("Bulk-sjekker tilgang (komplett) for saksbehandler $navIdent til ${request.personidenter.size} brukere")
        return tilgangsmaskinClient.sjekkTilgangBulk(
            personidenter = request.personidenter,
            regelType = RegelType.KOMPLETT_REGELTYPE,
        )
    }

    private fun tilForenkletRespons(respons: BulkTilgangsResponse) =
        ForenkletBulkTilgangsResponse(
            navIdent = respons.navIdent,
            resultater = respons.resultater.map { it.tilForenklet() },
        )

    private fun TilgangsResultat.tilForenklet(): ForenkletTilgangsResultat {
        if (status == 204) {
            return ForenkletTilgangsResultat(personident = personident, harTilgang = true)
        }

        val detaljer = detaljer as? Map<*, *>
        return ForenkletTilgangsResultat(
            personident = personident,
            harTilgang = false,
            avvisningskode =
                detaljer
                    ?.get("title")
                    ?.toString()
                    ?.let { runCatching { Avvisningskode.valueOf(it) }.getOrNull() },
            begrunnelse = detaljer?.get("begrunnelse")?.toString(),
        )
    }

    @Operation(summary = "Hent ansattinformasjon")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Ansattinformasjon hentet",
                content = [Content(schema = Schema(implementation = AnsattInfoResponse::class))],
            ),
        ],
    )
    @PostMapping("/ansatt", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAnsattInfo(
        @RequestBody request: AnsattInfoRequest,
    ): AnsattInfoResponse {
        logger.info("Henter info for ansatt ${request.navIdent}")
        return tilgangsmaskinClient.sjekkAnsatt(request.navIdent)
    }
}
