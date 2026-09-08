package no.nav.grunn.og.hjelpestonad.saksbehandler

import SaksbehandlerResponse
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class EntraProxyClient(
    @Value("\${ENTRA_PROXY_URL}") entraProxyUrl: String,
    @Value("\${ENTRA_PROXY_SCOPE}") private val entraProxyAudience: String,
    private val texasClient: TexasClient,
    restClientBuilder: RestClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(EntraProxyClient::class.java)
    private val restClient = restClientBuilder.clone().baseUrl(entraProxyUrl).build()

    fun hentSaksbehandlerInfo(navIdent: String): SaksbehandlerResponse {
        val oboToken =
            texasClient.hentOboToken(
                targetAudience = entraProxyAudience,
            )

        return try {
            restClient
                .get()
                .uri("/api/v1/ansatt/{navIdent}", navIdent)
                .headers { headers ->
                    headers.setBearerAuth(oboToken)
                    headers.set("X-Correlation-ID", MDC.get("callId") ?: "grunn-og-hjelpestonad")
                }.retrieve()
                .body<SaksbehandlerResponse>()
                ?.also { response ->
                    logger.info("Hentet saksbehandlerinfo for navIdent: {}", response.navIdent)
                } ?: throw RuntimeException("Klarte ikke å hente saksbehandlerinfo fra entra-proxy")
        } catch (e: Exception) {
            logger.error("Feilet å hente saksbehandlerinfo", e)
            throw e
        }
    }
}
