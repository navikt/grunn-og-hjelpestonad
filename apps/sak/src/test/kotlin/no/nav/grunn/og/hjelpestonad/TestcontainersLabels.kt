package no.nav.grunn.og.hjelpestonad

import org.testcontainers.containers.GenericContainer

// Global Testcontainers configuration for å gruppere containers under grunn-og-hjelpestonad i Docker Desktop
object TestcontainersLabels {
    private const val PROJECT_NAME = "grunn-og-hjelpestonad"

    fun <T : GenericContainer<*>> T.withProjectLabels(serviceName: String): T {
        this.withLabel("com.docker.compose.project", PROJECT_NAME)
        this.withLabel("com.docker.compose.service", serviceName)
        this.withCreateContainerCmdModifier { cmd ->
            cmd.withName("$PROJECT_NAME-$serviceName-1")
        }
        return this
    }
}
