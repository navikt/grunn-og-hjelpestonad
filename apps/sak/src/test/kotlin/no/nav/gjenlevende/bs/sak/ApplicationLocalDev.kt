package no.nav.gjenlevende.bs.sak

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

class ApplicationLocalDev : ApplicationLocalSetup()

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
    value = ["scheduling.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class SchedulingConfiguration

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationLocalDev::class.java)
        .profiles("local-dev")
        .run(*args)
}
