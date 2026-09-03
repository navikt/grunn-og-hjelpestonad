package no.nav.grunn.og.hjelpestonad.config

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan(
    "no.nav.familie.prosessering",
    "no.nav.grunn.og.hjelpestonad",
)
open class ApplicationConfig {
    // TODO
    @Bean
    open fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String = "Dummy"

        override fun harTilgang(): Boolean = true
    }
}
