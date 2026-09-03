package no.nav.gjenlevende.bs.sak.brev

import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI

@Component
class FamilieDokumentClient(
    @Value("\${FAMILIE_DOKUMENT_URL}") private val familieDokumentUrl: URI,
    @Value("\${FAMILIE_DOKUMENT_SCOPE}") private val familieDokumentScope: URI,
    private val texasClient: TexasClient,
) {
    val webClient =
        WebClient
            .builder()
            .baseUrl(familieDokumentUrl.toString())
            .defaultHeader("Content-Type", "application/json")
            .build()

    fun genererPdfFraHtml(html: String): ByteArray {
        val headers =
            HttpHeaders().apply {
                setBearerAuth(texasClient.hentMaskinToken(targetAudience = familieDokumentScope.toString()))
                this.contentType = MediaType.TEXT_HTML
                this.accept = listOf(MediaType.APPLICATION_PDF)
            }

        return webClient
            .post()
            .uri(HTML_TIL_PDF)
            .headers { it.addAll(headers) }
            .bodyValue(html.encodeToByteArray())
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: error("Ingen response ved henting av tilgang til person med relasjoner")
    }

    companion object {
        const val HTML_TIL_PDF = "api/html-til-pdf"
    }
}
