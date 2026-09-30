package no.nav.grunn.og.hjelpestonad.infotrygd

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PeriodeResponse
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.ResourceAccessException
import java.time.Duration
import java.time.LocalDate

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
                    .withRequestBody(equalToJson("""{"personidenter": ["12345678901"], "stønadstyper": []}"""))
                    .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "grunnstønad": [
                                        {
                                            "personident": "12345678901",
                                            "stønadstype": "GRUNNSTØNAD",
                                            "sakstype": "S",
                                            "kode": "F",
                                            "brukerId": "Z123456",
                                            "stønadId": 1,
                                            "vedtakId": 2,
                                            "vedtakstidspunkt": "2020-01-01T12:00:00",
                                            "vedtakKodeResultat": "I",
                                            "startDato": "2020-01-01",
                                            "innvilgetFom": "2020-01-01",
                                            "innvilgetTom": null,
                                            "opphørsdato": null,
                                            "oppdragId": null,
                                            "typeDelytelse": "E",
                                            "typeSats": "M",
                                            "typeUtbetaling": "M",
                                            "stønadFom": "2020-01-01",
                                            "stønadTom": null,
                                            "beløp": 1234.50,
                                            "trygdetidOgSats": []
                                        }
                                    ],
                                    "hjelpestønad": []
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val resultat = client.hentPerioderForPerson("12345678901")

            assertEquals(1, resultat.grunnstønad.size)
            assertEquals(PeriodeResponse.Stønadstype.GRUNNSTØNAD, resultat.grunnstønad.single().stønadstype)
            assertEquals(LocalDate.of(2020, 1, 1), resultat.grunnstønad.single().stønadFom)
            assertNull(resultat.grunnstønad.single().stønadTom)
            assertEquals(0, resultat.hjelpestønad.size)
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
                                    "grunnstønad": [],
                                    "hjelpestønad": []
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
