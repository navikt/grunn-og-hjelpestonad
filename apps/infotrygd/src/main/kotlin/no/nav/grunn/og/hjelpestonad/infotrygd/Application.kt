package no.nav.grunn.og.hjelpestonad.infotrygd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [DataJdbcRepositoriesAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
