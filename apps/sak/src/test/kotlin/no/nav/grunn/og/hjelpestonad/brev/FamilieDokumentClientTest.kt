package no.nav.grunn.og.hjelpestonad.brev

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

class FamilieDokumentClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun startServer() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun stopServer() {
            wireMockServer.stop()
        }
    }

    @Test
    fun `genererer PDF med maskintoken`() {
        val scope = URI.create("api://familie-dokument/.default")
        val texasClient = StubTexasClient()
        val pdf = "%PDF-test".encodeToByteArray()
        wireMockServer.stubFor(
            post(urlEqualTo("/api/html-til-pdf"))
                .withHeader("Authorization", equalTo("Bearer maskin-token"))
                .withHeader("Content-Type", equalTo("text/html"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdf),
                ),
        )
        val client =
            FamilieDokumentClient(
                familieDokumentUrl = URI.create("http://localhost:${wireMockServer.port()}"),
                familieDokumentScope = scope,
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )

        assertArrayEquals(pdf, client.genererPdfFraHtml("<p>Test</p>"))
        assertEquals(listOf(scope.toString()), texasClient.requestedMaskinAudiences)
    }
}
