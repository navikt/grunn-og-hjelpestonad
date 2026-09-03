package no.nav.gjenlevende.bs.sak.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
open class SafConfig(
    @Value("\${SAF_URL}") safBaseUri: URI,
    @Value("\${SAF_SCOPE}") val safScope: String,
) {
    val safBaseUri: URI = safBaseUri

    val safUri: URI =
        UriComponentsBuilder
            .fromUri(safBaseUri)
            .pathSegment(PATH_GRAPHQL)
            .build()
            .toUri()

    companion object {
        const val PATH_GRAPHQL = "graphql"

        val hentJournalposterBrukerQuery = graphqlQuery("/saf/journalposterForBruker.graphql")

        private fun graphqlQuery(path: String) =
            SafConfig::class.java
                ?.getResource(path)
                ?.readText()
                ?.graphqlCompatible() ?: throw IllegalStateException("Kunne ikke lese inn GraphQL query fra $path")

        private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
    }
}
