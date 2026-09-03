package no.nav.gjenlevende.bs.infotrygd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [DataJdbcRepositoriesAutoConfiguration::class])
open class Application

fun main(args: Array<String>) {
    System.setProperty("oracle.jdbc.fanEnabled", "false")
    runApplication<Application>(*args)
}
