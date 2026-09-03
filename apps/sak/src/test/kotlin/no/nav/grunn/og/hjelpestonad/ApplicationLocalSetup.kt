package no.nav.grunn.og.hjelpestonad

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
open class ApplicationLocalSetup
