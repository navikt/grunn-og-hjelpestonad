package no.nav.grunn.og.hjelpestonad.brev

import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class FamilieDokumentClient(
    @Value("\${FAMILIE_DOKUMENT_URL}") familieDokumentUrl: URI,
    @Value("\${FAMILIE_DOKUMENT_SCOPE}") private val familieDokumentScope: URI,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().baseUrl(familieDokumentUrl.toString()).build()

    fun genererPdfFraHtml(html: String): ByteArray =
        restClient
            .post()
            .uri(HTML_TIL_PDF)
            .headers { it.setBearerAuth(texasClient.hentMaskinToken(familieDokumentScope.toString())) }
            .contentType(MediaType.TEXT_HTML)
            .accept(MediaType.APPLICATION_PDF)
            .body(html.encodeToByteArray())
            .retrieve()
            .body<ByteArray>()
            ?: error("Ingen respons ved generering av PDF")

    companion object {
        const val HTML_TIL_PDF = "api/html-til-pdf"
    }
}
