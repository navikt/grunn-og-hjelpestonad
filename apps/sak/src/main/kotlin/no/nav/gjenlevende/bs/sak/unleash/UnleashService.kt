package no.nav.gjenlevende.bs.sak.unleash

import io.getunleash.Unleash
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("dev & !local")
@Service
class UnleashService(
    private val unleash: Unleash,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentFeatureToggles(): Map<String, Boolean> {
        val toggleNames = FeatureToggle.hentAlleFeatureToggleNavn()
        logger.info("Henter ${toggleNames.size} feature toggles fra Unleash")

        return toggleNames.associateWith { toggleName ->
            val enabled = unleash.isEnabled(toggleName)
            enabled
        }
    }
}
