package no.nav.grunn.og.hjelpestonad.iverksett.brev

import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.ArkiverDokumentResponse
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostRequest
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class DokarkivClient(
    @Value("\${integrasjoner.dokarkiv.url}") dokarkivUrl: URI,
    @Value("\${integrasjoner.dokarkiv.scope}") private val dokarkivScope: URI,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().baseUrl(dokarkivUrl.toString()).build()

    fun arkiverDokument(
        journalpostRequest: JournalpostRequest,
        forsoekFerdigstill: Boolean,
    ): ArkiverDokumentResponse =
        restClient
            .post()
            .uri { it.path(OPPRETT_JOURNALPOST).queryParam("forsoekFerdigstill", forsoekFerdigstill).build() }
            .headers { it.setBearerAuth(texasClient.hentMaskinToken(dokarkivScope.toString())) }
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(journalpostRequest)
            .retrieve()
            .body<ArkiverDokumentResponse>()
            ?: error("Ingen respons ved arkivering av dokument")

    companion object {
        const val OPPRETT_JOURNALPOST = "rest/journalpostapi/v1/journalpost"
    }
}
