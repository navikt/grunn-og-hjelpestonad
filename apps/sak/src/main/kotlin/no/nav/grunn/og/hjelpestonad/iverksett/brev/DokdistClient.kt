package no.nav.grunn.og.hjelpestonad.iverksett.brev

import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DistribuerJournalpostResponse
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class DokdistClient(
    @Value("\${DOKDIST_URL}") dokdistUrl: URI,
    @Value("\${DOKDIST_SCOPE}") private val dokdistScope: URI,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().baseUrl(dokdistUrl.toString()).build()

    fun distribuerDokument(distribuerJournalpostRequest: DistribuerJournalpostRequest) {
        restClient
            .post()
            .uri(DISTRIBUER_DOKUMENT)
            .headers { it.setBearerAuth(texasClient.hentMaskinToken(dokdistScope.toString())) }
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(distribuerJournalpostRequest)
            .retrieve()
            .body(DistribuerJournalpostResponse::class.java)
            ?: error("Ingen respons ved distribusjon av dokument")
    }

    companion object {
        const val DISTRIBUER_DOKUMENT = "rest/v1/distribuerjournalpost"
    }
}
