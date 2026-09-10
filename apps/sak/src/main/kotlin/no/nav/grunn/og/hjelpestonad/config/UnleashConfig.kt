package no.nav.grunn.og.hjelpestonad.config

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!integrasjonstest & !local & !local-mock")
@Configuration
open class UnleashConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${unleash.api.url}")
    private lateinit var unleashUrl: String

    @Value("\${unleash.api.token}")
    private lateinit var unleashToken: String

    @Value("\${nais.app-navn}")
    private lateinit var appName: String

    @Bean
    open fun unleash(): Unleash {
        val config =
            UnleashConfig
                .builder()
                .appName(appName)
                .instanceId(appName)
                .unleashAPI(unleashUrl)
                .apiKey(unleashToken)
                .build()

        logger.info("Konfigurerer Unleash med URL: $unleashUrl, App: $appName, Miljø: ${config.environment}")

        val unleash = DefaultUnleash(config)

        return unleash
    }
}
