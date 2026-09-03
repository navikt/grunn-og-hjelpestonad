package no.nav.gjenlevende.bs.sak.infotrygd

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

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
                infotrygdWebClient =
                    WebClient
                        .builder()
                        .baseUrl("http://localhost:${wireMockServer.port()}")
                        .defaultHeader("Content-Type", "application/json")
                        .build(),
                texasClient = texasClient,
                gjenlevendeBsInfotrygdAudience = AUDIENCE,
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
        fun `returnerer perioder ved vellykket kall`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
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

            val resultat = client.hentPerioderForPersonSync("12345678901")

            assertNotNull(resultat)
            assertEquals("12345678901", resultat.personident)
        }

        @Test
        fun `returnerer tom Mono ved 404 person ikke funnet`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/perioder"))
                    .willReturn(
                        aResponse()
                            .withStatus(404),
                    ),
            )

            val resultat = client.hentPerioderForPerson("12345678901").block()

            assertNull(resultat)
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
                client.hentPerioderForPersonSync("12345678901")
            }
        }
    }
}
