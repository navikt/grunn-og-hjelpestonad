package no.nav.grunn.og.hjelpestonad.oppdrag

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

class OppdragClientTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer
        private const val SCOPE = "api://test-oppdrag/.default"
        private const val PATH = "/api/simulering/v1"
        private const val IVERKSETT_PATH = "/api/oppdrag"
        private const val STATUS_PATH = "/api/status"

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
    private lateinit var client: OppdragClient

    private val utbetalingsoppdrag =
        lagUtbetalingsoppdrag(
            Oppdragsgrunnlag(
                behandlingId = UUID.randomUUID(),
                eksternBehandlingId = 1L,
                eksternFagsakId = 2L,
                personIdent = "12345678910",
                saksbehandlerId = "Z123456",
                vedtaksdato = LocalDate.of(2026, 1, 15),
                andelerTilkjentYtelse =
                    listOf(
                        AndelTilkjentYtelse(
                            beløp = 1234.toBigDecimal(),
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 6, 30),
                            kildeBehandlingId = UUID.randomUUID(),
                        ),
                    ),
            ),
        ).utbetalingsoppdrag

    @BeforeEach
    fun setup() {
        client = lagKlient()
        every { texasClient.hentOboToken(SCOPE) } returns "gyldig-token"
        every { texasClient.hentMaskinToken(SCOPE) } returns "app-token"
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    private fun lagKlient(readTimeout: Duration = Duration.ofSeconds(10)) =
        OppdragClient(
            texasClient = texasClient,
            restClientBuilder = testRestClientBuilder(readTimeout = readTimeout),
            url = "http://localhost:${wireMockServer.port()}",
            scope = SCOPE,
        )

    private fun stubSvar(
        body: String,
        status: Int = 200,
        forsinkelseMs: Int = 0,
        path: String = PATH,
    ) {
        wireMockServer.stubFor(
            post(urlEqualTo(path)).willReturn(
                aResponse()
                    .withStatus(status)
                    .withFixedDelay(forsinkelseMs)
                    .withHeader("Content-Type", "application/json")
                    .withBody(body),
            ),
        )
    }

    @Test
    fun `sender oppdraget med OBO-token og leser simuleringsresultatet`() {
        stubSvar(
            """
            {
              "data": {
                "simuleringMottaker": [
                  {
                    "simulertPostering": [
                      {
                        "fagOmrådeKode": "IT28",
                        "fom": "2026-01-01",
                        "tom": "2026-01-31",
                        "betalingType": "DEBIT",
                        "beløp": 1234,
                        "posteringType": "YTELSE",
                        "forfallsdato": "2026-01-20",
                        "utenInntrekk": false,
                        "ukjentFelt": "x"
                      }
                    ],
                    "mottakerNummer": "12345678910",
                    "mottakerType": "BRUKER"
                  }
                ]
              },
              "status": "SUKSESS",
              "melding": "Innhenting av data var vellykket"
            }
            """.trimIndent(),
        )

        val resultat = client.simuler(utbetalingsoppdrag)

        val postering =
            resultat.simuleringMottaker
                .single()
                .simulertPostering
                .single()
        assertEquals(0, BigDecimal(1234).compareTo(postering.beløp))
        assertEquals("IT28", postering.fagOmrådeKode)
        verify(exactly = 1) { texasClient.hentOboToken(SCOPE) }
        verify(exactly = 0) { texasClient.hentMaskinToken(any()) }
        wireMockServer.verify(
            postRequestedFor(urlEqualTo(PATH))
                .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                .withRequestBody(matchingJsonPath("$.fagSystem", equalTo("IT28")))
                .withRequestBody(matchingJsonPath("$.kodeEndring", equalTo("NY")))
                .withRequestBody(matchingJsonPath("$.utbetalingsperiode[0].klassifisering", equalTo("GHGSORMG")))
                .withRequestBody(matchingJsonPath("$.utbetalingsperiode[0].vedtakdatoFom", equalTo("2026-01-01")))
                .withRequestBody(matchingJsonPath("$.utbetalingsperiode[0].datoForVedtak", equalTo("2026-01-15")))
                .withRequestBody(matchingJsonPath("$.avstemmingTidspunkt", matching("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
                .withRequestBody(matchingJsonPath("$.utbetalingsperiode[0].sats", equalTo("1234"))),
        )
    }

    @Test
    fun `status FEILET i svaret gir Feil med bad gateway`() {
        stubSvar("""{"data": null, "status": "FEILET", "melding": "OS er stengt"}""")

        val feil = assertThrows(Feil::class.java) { client.simuler(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", """{"data": null, "status": "SUKSESS"}"""])
    fun `tomt svar eller manglende data gir Feil med bad gateway`(body: String) {
        stubSvar(body)

        val feil = assertThrows(Feil::class.java) { client.simuler(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @Test
    fun `serverfeil gir Feil med bad gateway`() {
        stubSvar("""{"data": null, "status": "FEILET", "melding": "Noe gikk galt"}""", status = 500)

        val feil = assertThrows(Feil::class.java) { client.simuler(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @Test
    fun `responstimeout gir Feil med bad gateway`() {
        stubSvar("""{"data": {"simuleringMottaker": []}, "status": "SUKSESS"}""", forsinkelseMs = 500)

        val feil = assertThrows(Feil::class.java) { lagKlient(readTimeout = Duration.ofMillis(100)).simuler(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @Test
    fun `ugyldig JSON gir Feil med bad gateway`() {
        stubSvar("""{"data": ikke json""")

        val feil = assertThrows(Feil::class.java) { client.simuler(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @Test
    fun `iverksett sender oppdraget med apptoken til api oppdrag`() {
        stubSvar("""{"data": "Oppdrag sendt OK", "status": "SUKSESS", "melding": "Innhenting av data var vellykket"}""", path = IVERKSETT_PATH)

        val utfall = client.iverksett(utbetalingsoppdrag)

        assertEquals(IverksettUtfall.SENDT, utfall)
        verify(exactly = 0) { texasClient.hentOboToken(any()) }
        wireMockServer.verify(
            postRequestedFor(urlEqualTo(IVERKSETT_PATH))
                .withHeader("Authorization", equalTo("Bearer app-token"))
                .withRequestBody(matchingJsonPath("$.fagSystem", equalTo("IT28")))
                .withRequestBody(matchingJsonPath("$.saksnummer", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.utbetalingsperiode[0].behandlingId", equalTo("1"))),
        )
    }

    @Test
    fun `iverksett gir ALLEREDE_SENDT når oppdraget er sendt før`() {
        stubSvar("""{"data": null, "status": "FEILET", "melding": "Oppdrag er allerede sendt for saksnr 2."}""", status = 409, path = IVERKSETT_PATH)

        assertEquals(IverksettUtfall.ALLEREDE_SENDT, client.iverksett(utbetalingsoppdrag))
    }

    @Test
    fun `iverksett gir bad gateway når familie-oppdrag-backend ikke klarer å sende`() {
        stubSvar("""{"data": null, "status": "FEILET", "melding": "Klarte ikke sende oppdrag for saksnr 2"}""", status = 500, path = IVERKSETT_PATH)

        val feil = assertThrows(Feil::class.java) { client.iverksett(utbetalingsoppdrag) }

        assertEquals(HttpStatus.BAD_GATEWAY, feil.httpStatus)
    }

    @Test
    fun `hentStatus sender OppdragId og leser status og kvitteringsmelding`() {
        stubSvar("""{"data": "KVITTERT_OK", "status": "SUKSESS", "melding": "Oppdrag behandlet"}""", path = STATUS_PATH)

        val svar = client.hentStatus(OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "1"))

        assertEquals(OppdragStatusSvar(status = "KVITTERT_OK", melding = "Oppdrag behandlet"), svar)
        wireMockServer.verify(
            postRequestedFor(urlEqualTo(STATUS_PATH))
                .withHeader("Authorization", equalTo("Bearer app-token"))
                .withRequestBody(matchingJsonPath("$.fagsystem", equalTo("IT28")))
                .withRequestBody(matchingJsonPath("$.personIdent", equalTo("12345678910")))
                .withRequestBody(matchingJsonPath("$.behandlingsId", equalTo("1"))),
        )
    }

    @Test
    fun `hentStatus gir melding null når kvitteringen mangler beskrivelse`() {
        stubSvar("""{"data": "LAGT_PÅ_KØ", "status": "SUKSESS", "melding": ""}""", path = STATUS_PATH)

        val svar = client.hentStatus(OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "1"))

        assertEquals(OppdragStatusSvar(status = "LAGT_PÅ_KØ", melding = null), svar)
    }

    @Test
    fun `hentStatus gir not found når oppdraget ikke finnes`() {
        stubSvar("""{"data": null, "status": "FEILET", "melding": "Fant ikke oppdrag"}""", status = 404, path = STATUS_PATH)

        val feil = assertThrows(Feil::class.java) { client.hentStatus(OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "1")) }

        assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
    }
}
