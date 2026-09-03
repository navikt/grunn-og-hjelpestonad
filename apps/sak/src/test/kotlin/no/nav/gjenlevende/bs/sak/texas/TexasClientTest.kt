package no.nav.gjenlevende.bs.sak.texas

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TexasClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var client: TexasClient

        val brukerToken = "test-user-token-xyz"

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()

            client =
                TexasClient(
                    tokenExchangeEndpoint = "http://localhost:${wireMockServer.port()}/token/exchange",
                    tokenMachineEndpoint = "http://localhost:${wireMockServer.port()}/token",
                )
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    @BeforeEach
    fun setupEachTest() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentBrukerToken() } returns brukerToken
    }

    @AfterEach
    fun tearDownEachTest() {
        unmockkObject(SikkerhetContext)
        wireMockServer.resetAll()
    }

    @Nested
    inner class HentOboToken {
        @Test
        fun `returnerer token ved vellykket kall`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/token/exchange"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "access_token": "$brukerToken",
                                    "expires_in": 3600,
                                    "token_type": "Bearer"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val token =
                client.hentOboToken(
                    targetAudience = "api://target/.default",
                )

            assertEquals(brukerToken, token)
        }

        @Test
        fun `kaster exception ved tomt token`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/token/exchange"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "access_token": "",
                                    "expires_in": 3600,
                                    "token_type": "Bearer"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val exception =
                assertThrows(RuntimeException::class.java) {
                    client.hentOboToken(
                        targetAudience = "api://target/.default",
                    )
                }

            assertEquals("Texas returnerte tomt access_token", exception.message)
        }

        @Test
        fun `kaster exception ved HTTP feil fra Texas`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/token/exchange"))
                    .willReturn(
                        aResponse()
                            .withStatus(400)
                            .withBody("Bad Request"),
                    ),
            )

            val exception =
                assertThrows(RuntimeException::class.java) {
                    client.hentOboToken(
                        targetAudience = "api://target/.default",
                    )
                }

            assertEquals("Kunne ikke bytte token via Texas OBO: HTTP 400 BAD_REQUEST", exception.message)
        }
    }

    @Nested
    inner class HentMaskinToken {
        @Test
        fun `returnerer token ved vellykket respons`() {
            val forventetToken = "test-maskin-token-456"

            wireMockServer.stubFor(
                post(urlEqualTo("/token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "access_token": "$forventetToken",
                                    "expires_in": 3600,
                                    "token_type": "Bearer"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val token = client.hentMaskinToken(targetAudience = "api://target/.default")

            assertEquals(forventetToken, token)
        }

        @Test
        fun `kaster exception ved tomt token`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "access_token": "",
                                    "expires_in": 3600,
                                    "token_type": "Bearer"
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val exception =
                assertThrows(RuntimeException::class.java) {
                    client.hentMaskinToken(targetAudience = "api://target/.default")
                }

            assertEquals("Texas returnerte tomt access_token", exception.message)
        }

        @Test
        fun `kaster exception ved HTTP feil fra Texas`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/token"))
                    .willReturn(
                        aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error"),
                    ),
            )

            val exception =
                assertThrows(RuntimeException::class.java) {
                    client.hentMaskinToken(targetAudience = "api://target/.default")
                }

            assertEquals("Kunne ikke hente maskintoken via Texas: HTTP 500 INTERNAL_SERVER_ERROR", exception.message)
        }
    }
}
