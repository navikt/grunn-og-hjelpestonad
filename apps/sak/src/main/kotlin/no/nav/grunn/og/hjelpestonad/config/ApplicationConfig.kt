package no.nav.grunn.og.hjelpestonad.config

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan(
    "no.nav.grunn.og.hjelpestonad",
)
open class ApplicationConfig
