package no.nav.gjenlevende.bs.sak

import org.testcontainers.containers.GenericContainer

// Global Testcontainers configuration for å gruppere containers under gjenlevende-bs-sak i Docker Desktop
object TestcontainersLabels {
    private const val PROJECT_NAME = "gjenlevende-bs-sak"

    fun <T : GenericContainer<*>> T.withProjectLabels(serviceName: String): T {
        this.withLabel("com.docker.compose.project", PROJECT_NAME)
        this.withLabel("com.docker.compose.service", serviceName)
        this.withCreateContainerCmdModifier { cmd ->
            cmd.withName("$PROJECT_NAME-$serviceName-1")
        }
        return this
    }
}
