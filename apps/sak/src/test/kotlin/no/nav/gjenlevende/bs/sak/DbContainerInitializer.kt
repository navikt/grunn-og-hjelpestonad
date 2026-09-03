package no.nav.gjenlevende.bs.sak

import no.nav.gjenlevende.bs.sak.TestcontainersLabels.withProjectLabels
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

class DbContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgres.start()
    }

    companion object {
        private val postgres: KPostgreSQLContainer by lazy {
            KPostgreSQLContainer("postgres:17.6")
                .withDatabaseName("gjenlevende-bs-sak")
                .withUsername("postgres")
                .withPassword("test")
                .withProjectLabels("test-database")
        }
    }
}

class KPostgreSQLContainer(
    imageName: String,
) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
