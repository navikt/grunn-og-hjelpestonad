package no.nav.grunn.og.hjelpestonad.infotrygd

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
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import java.time.Duration

class InfotrygdClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer
        private const val AUDIENCE = "api://test/.default"

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

    private val texasClient = mockk<TexasClient>()
    private lateinit var client: InfotrygdClient

    @BeforeEach
    fun setup() {
        client =
            InfotrygdClient(
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
                infotrygdUrl = "http://localhost:${wireMockServer.port()}",
                grunnOgHjelpestonadInfotrygdAudience = AUDIENCE,
            )
        every { texasClient.hentOboToken(AUDIENCE) } returns "gyldig-token"
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    @Nested
    inner class HentPerioderForPerson {
        @Test
        fun `returnerer perioder og sender OBO-token ved vellykket kall`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "personident": "12345678901",
                                    "barnetilsyn": [],
                                    "skolepenger": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val resultat = client.hentPerioderForPerson("12345678901")

            assertEquals("12345678901", resultat.personident)
            verify(exactly = 1) { texasClient.hentOboToken(AUDIENCE) }
        }

        @Test
        fun `kaster Feil med not found ved 404`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .willReturn(aResponse().withStatus(404)),
            )

            val feil =
                assertThrows(Feil::class.java) {
                    client.hentPerioderForPerson("12345678901")
                }

            assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
        }

        @Test
        fun `kaster Feil med not found ved tom respons`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .willReturn(aResponse().withStatus(204)),
            )

            val feil =
                assertThrows(Feil::class.java) {
                    client.hentPerioderForPerson("12345678901")
                }

            assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
        }

        @Test
        fun `kaster exception ved serverfeil`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .willReturn(
                        aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error"),
                    ),
            )

            assertThrows(Exception::class.java) {
                client.hentPerioderForPerson("12345678901")
            }
        }

        @Test
        fun `kaster ResourceAccessException ved responstimeout`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .willReturn(
                        aResponse()
                            .withFixedDelay(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "personident": "12345678901",
                                    "barnetilsyn": [],
                                    "skolepenger": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )
            val timeoutClient =
                InfotrygdClient(
                    texasClient = texasClient,
                    restClientBuilder = testRestClientBuilder(readTimeout = Duration.ofMillis(100)),
                    infotrygdUrl = "http://localhost:${wireMockServer.port()}",
                    grunnOgHjelpestonadInfotrygdAudience = AUDIENCE,
                )

            assertThrows(ResourceAccessException::class.java) {
                timeoutClient.hentPerioderForPerson("12345678901")
            }
        }
    }
}
