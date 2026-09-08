package no.nav.grunn.og.hjelpestonad.saksbehandler

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EntraProxyClientTest {
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
    fun `henter saksbehandler med OBO-token`() {
        val audience = "api://entra-proxy/.default"
        val texasClient = StubTexasClient()
        wireMockServer.stubFor(
            get(urlEqualTo("/api/v1/ansatt/Z123456"))
                .withHeader("Authorization", equalTo("Bearer obo-token"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "navIdent": "Z123456",
                                "visningNavn": "Test Saksbehandler",
                                "fornavn": "Test",
                                "etternavn": "Saksbehandler",
                                "tIdent": "T123456",
                                "epost": "test@example.com",
                                "enhet": {
                                    "enhetnummer": "1234",
                                    "navn": "Testenhet"
                                }
                            }
                            """.trimIndent(),
                        ),
                ),
        )
        val client =
            EntraProxyClient(
                entraProxyUrl = "http://localhost:${wireMockServer.port()}",
                entraProxyAudience = audience,
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )

        assertEquals("Z123456", client.hentSaksbehandlerInfo("Z123456").navIdent)
        assertEquals(listOf(audience), texasClient.requestedOboAudiences)
    }
}
