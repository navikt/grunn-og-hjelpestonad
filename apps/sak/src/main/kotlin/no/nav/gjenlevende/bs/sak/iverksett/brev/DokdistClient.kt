package no.nav.gjenlevende.bs.sak.iverksett.brev

import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.DistribuerJournalpostResponse
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI

@Component
class DokdistClient(
    @Value("\${DOKDIST_URL}") private val dokdistUrl: URI,
    @Value("\${DOKDIST_SCOPE}") private val dokdistScope: URI,
    private val texasClient: TexasClient,
) {
    val webClient =
        WebClient
            .builder()
            .baseUrl(dokdistUrl.toString())
            .defaultHeader("Content-Type", "application/json")
            .build()

    fun distribuerDokument(distribuerJournalpostRequest: DistribuerJournalpostRequest) {
        val headers =
            HttpHeaders().apply {
                setBearerAuth(texasClient.hentMaskinToken(targetAudience = dokdistScope.toString()))
                this.contentType = MediaType.APPLICATION_JSON
                this.accept = listOf(MediaType.APPLICATION_JSON)
            }
        webClient
            .post()
            .uri { it.path(DISTRIBUER_DOKUMENT).build() }
            .headers { it.addAll(headers) }
            .bodyValue(distribuerJournalpostRequest)
            .retrieve()
            .bodyToMono<DistribuerJournalpostResponse>()
            .block() ?: error("Ingen respons ved distribusjon av dokument")
    }

    companion object {
        const val DISTRIBUER_DOKUMENT = "rest/v1/distribuerjournalpost"
    }
}
