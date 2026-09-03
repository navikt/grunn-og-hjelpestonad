package no.nav.gjenlevende.bs.sak.unleash

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("dev & !local")
@RestController
@RequestMapping("/api/unleash")
@Tag(name = "Unleash", description = "Feature toggle API")
class UnleashController(
    private val unleashService: UnleashService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/toggles")
    @Operation(
        summary = "Hent alle feature toggles",
        description = "Returnerer en map med alle definerte feature toggles og deres status",
    )
    fun hentFeatureToggles(): ResponseEntity<Map<String, Boolean>> {
        val toggles = unleashService.hentFeatureToggles()
        logger.info("Returnerer ${toggles.size} feature toggles til frontend")
        return ResponseEntity.ok(toggles)
    }
}
