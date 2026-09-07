package no.nav.grunn.og.hjelpestonad

import no.nav.grunn.og.hjelpestonad.TestcontainersLabels.withProjectLabels
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
                .withDatabaseName("grunn-og-hjelpestonad")
                .withUsername("postgres")
                .withPassword("test")
                .withProjectLabels("test-database")
        }
    }
}

class KPostgreSQLContainer(
    imageName: String,
) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
