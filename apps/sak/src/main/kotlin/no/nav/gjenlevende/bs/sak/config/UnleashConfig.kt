package no.nav.gjenlevende.bs.sak.config

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!integrasjonstest & !local & !local-mock & !local-dev")
@Configuration
open class UnleashConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${UNLEASH_SERVER_API_URL}")
    private lateinit var unleashUrl: String

    @Value("\${UNLEASH_SERVER_API_TOKEN}")
    private lateinit var unleashToken: String

    @Value("\${NAIS_APP_NAME}")
    private lateinit var appName: String

    @Value("\${UNLEASH_SERVER_API_ENV}")
    private lateinit var environment: String

    @Bean
    open fun unleash(): Unleash {
        logger.info("Konfigurerer Unleash med URL: $unleashUrl, App: $appName, Miljø: $environment")

        val config =
            UnleashConfig
                .builder()
                .appName(appName)
                .instanceId(appName)
                .unleashAPI("$unleashUrl/api")
                .apiKey(unleashToken)
                .build()

        val unleash = DefaultUnleash(config)

        return unleash
    }
}
