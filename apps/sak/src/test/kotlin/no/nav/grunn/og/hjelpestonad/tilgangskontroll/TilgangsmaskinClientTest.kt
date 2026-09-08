package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class TilgangsmaskinClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    private val tilgangsmaskinScope = "api://test/.default"
    private val texasClient = mockk<TexasClient>()
    private lateinit var client: TilgangsmaskinClient

    @BeforeEach
    fun setup() {
        client =
            TilgangsmaskinClient(
                tilgangsmaskinUrl = URI.create("http://localhost:${wireMockServer.port()}"),
                tilgangsmaskinScope = tilgangsmaskinScope,
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )
        every { texasClient.hentOboToken(tilgangsmaskinScope) } returns "gyldig-token"
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    @Nested
    inner class SjekkTilgangBulk {
        @Test
        fun `returnerer manglende tilgang`() {
            stubBulkResponse(status = 403)

            val resultat = client.sjekkTilgangBulk(listOf("12345678901"))

            assertTrue(resultat.resultater.none { it.harTilgang })
        }

        @Test
        fun `returnerer tilgang og sender OBO-token`() {
            stubBulkResponse(status = 204)

            val resultat = client.sjekkTilgangBulk(listOf("12345678901"))

            assertTrue(resultat.resultater.all { it.harTilgang })
            verify(exactly = 1) { texasClient.hentOboToken(tilgangsmaskinScope) }
        }

        private fun stubBulkResponse(status: Int) {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/v1/bulk/obo/KJERNE_REGELTYPE"))
                    .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "ansattId": "Z123456",
                                    "resultater": [
                                        {
                                            "brukerId": "12345678901",
                                            "status": $status,
                                            "detaljer": null
                                        }
                                    ]
                                }
                                """.trimIndent(),
                            ),
                    ),
            )
        }
    }
}
