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
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderRequest
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
    inner class HentStønader {
        private fun hentGrunnstønad(klient: InfotrygdClient = client) = klient.hentStønader("12345678901", StonaderRequest.Stønadstype.GRUNNSTØNAD)

        @Test
        fun `returnerer stønadstreet og sender OBO-token ved vellykket kall`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .withRequestBody(equalToJson("""{"personidenter": ["12345678901"], "stønadstype": "GRUNNSTØNAD"}"""))
                    .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "stonader": [
                                        {
                                            "stonadId": 1,
                                            "personnr": "12345678901",
                                            "kodeRutine": "GS",
                                            "datoStart": "2020-01-01",
                                            "kodeOpphor": null,
                                            "datoOpphor": null,
                                            "oppdragId": 10,
                                            "tidspunktOpphort": null,
                                            "tidspunktReg": "2020-01-01T12:00:00",
                                            "brukerid": "Z123456",
                                            "vedtak": [
                                                {
                                                    "vedtakId": 2,
                                                    "tknr": "0301",
                                                    "saksblokk": "A",
                                                    "saksnr": 1,
                                                    "typeSak": "S",
                                                    "kodeResultat": "I",
                                                    "datoInnvFom": "2020-01-01",
                                                    "datoInnvTom": null,
                                                    "datoMottattSak": "2019-12-15",
                                                    "kodeVedtaksnivaa": "L",
                                                    "typeBeregning": "M",
                                                    "tidspunktReg": "2020-01-01T12:00:00",
                                                    "brukerid": "Z123456",
                                                    "endringer": ["F"],
                                                    "delytelser": [
                                                        {
                                                            "typeDelytelse": "GS",
                                                            "tidspunktReg": "2020-01-01T12:00:00",
                                                            "fom": "2020-01-01",
                                                            "tom": null,
                                                            "belop": 1234.50,
                                                            "typeSats": "M",
                                                            "typeUtbetaling": "J",
                                                            "brukerid": "Z123456",
                                                            "linjeId": null
                                                        }
                                                    ],
                                                    "gh": [],
                                                    "evaluering": null
                                                }
                                            ]
                                        }
                                    ]
                                }
                                """.trimIndent(),
                            ),
                    ),
            )

            val resultat = hentGrunnstønad()

            val vedtak =
                resultat.stonader
                    .single()
                    .vedtak
                    .single()
            assertEquals(LocalDate.of(2020, 1, 1), vedtak.datoInnvFom)
            assertNull(vedtak.datoInnvTom)
            assertEquals(listOf("F"), vedtak.endringer)
            assertEquals(1, vedtak.delytelser.size)
            verify(exactly = 1) { texasClient.hentOboToken(AUDIENCE) }
        }

        @Test
        fun `tom liste er et gyldig svar`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{"stonader": []}"""),
                    ),
            )

            assertEquals(emptyList<Any>(), hentGrunnstønad().stonader)
        }

        @Test
        fun `kaster Feil med not found ved 404`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(aResponse().withStatus(404)),
            )

            val feil = assertThrows(Feil::class.java) { hentGrunnstønad() }

            assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
        }

        @Test
        fun `kaster Feil med not found ved tom respons`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(aResponse().withStatus(204)),
            )

            val feil = assertThrows(Feil::class.java) { hentGrunnstønad() }

            assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
        }

        @Test
        fun `kaster exception ved serverfeil`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(
                        aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error"),
                    ),
            )

            assertThrows(Exception::class.java) { hentGrunnstønad() }
        }

        @Test
        fun `kaster Feil ved responstimeout`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(
                        aResponse()
                            .withFixedDelay(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{"stonader": []}"""),
                    ),
            )
            val timeoutClient =
                InfotrygdClient(
                    texasClient = texasClient,
                    restClientBuilder = testRestClientBuilder(readTimeout = Duration.ofMillis(100)),
                    infotrygdUrl = "http://localhost:${wireMockServer.port()}",
                    grunnOgHjelpestonadInfotrygdAudience = AUDIENCE,
                )

            val feil = assertThrows(Feil::class.java) { hentGrunnstønad(timeoutClient) }

            assertEquals("Kunne ikke hente stønader fra Infotrygd", feil.message)
        }

        @Test
        fun `ugyldig JSON gir Feil uten cause og med generell melding`() {
            wireMockServer.stubFor(
                post(urlEqualTo("/api/infotrygd/stonader"))
                    .willReturn(
                        aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{"stonader": [{"diagnose1": "sensitive-value"}]}"""),
                    ),
            )

            val feil = assertThrows(Feil::class.java) { hentGrunnstønad() }

            assertEquals("Kunne ikke hente stønader fra Infotrygd", feil.message)
            assertNull(feil.cause)
        }
    }
}
