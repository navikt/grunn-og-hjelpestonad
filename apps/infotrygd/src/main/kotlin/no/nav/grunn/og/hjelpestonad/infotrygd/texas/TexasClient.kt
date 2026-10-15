package no.nav.grunn.og.hjelpestonad.infotrygd.texas

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.grunn.og.hjelpestonad.infotrygd.security.SikkerhetContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
open class TexasClient(
    @Value("\${nais.texas.token-endpoint}")
    private val tokenEndpoint: String,
    @Value("\${nais.texas.token-exchange-endpoint}")
    private val tokenExchangeEndpoint: String,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder.clone().build()

    open fun hentOboToken(target: String): String {
        val skjema =
            LinkedMultiValueMap<String, String>().apply {
                add("identity_provider", "entra_id")
                add("target", target)
                add("user_token", SikkerhetContext.hentBrukerToken())
            }

        val token =
            restClient
                .post()
                .uri(tokenExchangeEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(skjema)
                .retrieve()
                .body<TexasTokenResponse>()
                ?.accessToken

        check(!token.isNullOrBlank()) { "Texas returnerte tomt access_token for $target" }
        return token
    }

    open fun hentMaskinToken(scope: String): String {
        val skjema =
            LinkedMultiValueMap<String, String>().apply {
                add("identity_provider", "entra_id")
                add("target", scope)
            }

        val token =
            restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(skjema)
                .retrieve()
                .body<TexasTokenResponse>()
                ?.accessToken

        check(!token.isNullOrBlank()) { "Texas returnerte tomt access_token for $scope" }
        return token
    }
}

private data class TexasTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
)
