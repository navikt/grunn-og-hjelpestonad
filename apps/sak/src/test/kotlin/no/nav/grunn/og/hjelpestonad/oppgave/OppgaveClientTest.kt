package no.nav.grunn.og.hjelpestonad.oppgave

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.util.UUID

class OppgaveClientTest {
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

    private lateinit var client: OppgaveClient
    private lateinit var texasClient: StubTexasClient
    private val oppgaveScope = URI.create("api://oppgave/.default")
    private val idempotensUuid = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        texasClient = StubTexasClient()
        client =
            OppgaveClient(
                texasClient = texasClient,
                oppgaveUrl = "http://localhost:${wireMockServer.port()}",
                oppgaveScope = oppgaveScope,
                restClientBuilder = testRestClientBuilder(),
            )
    }

    @AfterEach
    fun resetServer() {
        wireMockServer.resetAll()
    }

    @Test
    fun `oppretter oppgave med maskintoken og correlation-id`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/oppgaver"))
                .withHeader("Authorization", equalTo("Bearer maskin-token"))
                .withHeader("X-Correlation-ID", matching(".+"))
                .withRequestBody(matchingJsonPath("$.tema", equalTo("GRU")))
                .withRequestBody(matchingJsonPath("$.behandlingstema", equalTo("ab0132")))
                .withRequestBody(matchingJsonPath("$.oppgavetype", equalTo("BEH_SAK")))
                .withRequestBody(matchingJsonPath("$.uuid", equalTo(idempotensUuid.toString())))
                .willReturn(jsonResponse(oppgaveResponse()).withStatus(201)),
        )

        val response =
            client.opprettOppgaveM2M(
                opprettelsesrequest(),
            )

        assertEquals(123, response.id)
        assertEquals(listOf(oppgaveScope.toString()), texasClient.requestedMaskinAudiences)
    }

    @Test
    fun `gjentatt opprettelse henter eksisterende oppgave etter tapt svar`() {
        val request = opprettelsesrequest()
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/oppgaver")).willReturn(aResponse().withStatus(503)))
        assertThrows(RestClientResponseException::class.java) { client.opprettOppgaveM2M(request) }

        stubDuplikatsvar()
        stubOppgaveForDuplikat("sak-1")

        assertEquals(123L, client.opprettOppgaveM2M(request).id)
        wireMockServer.verify(
            2,
            postRequestedFor(urlEqualTo("/api/v1/oppgaver"))
                .withRequestBody(matchingJsonPath("$.uuid", equalTo(idempotensUuid.toString()))),
        )
    }

    @Test
    fun `avviser oppgave som tilhører en annen sak`() {
        stubDuplikatsvar()
        stubOppgaveForDuplikat("annen-sak")

        assertThrows(IllegalStateException::class.java) { client.opprettOppgaveM2M(opprettelsesrequest()) }
    }

    @Test
    fun `godtar konflikt med endret feiltekst når oppgaveId peker til samme sak`() {
        stubKonflikt("""{"oppgaveId":123,"feilmelding":"Endret feiltekst fra Oppgave"}""")
        stubOppgaveForDuplikat("sak-1")

        assertEquals(123L, client.opprettOppgaveM2M(opprettelsesrequest()).id)
    }

    @Test
    fun `godtar konflikt uten feiltekst når oppgaveId peker til samme sak`() {
        stubKonflikt("""{"oppgaveId":123}""")
        stubOppgaveForDuplikat("sak-1")

        assertEquals(123L, client.opprettOppgaveM2M(opprettelsesrequest()).id)
    }

    @ParameterizedTest
    @ValueSource(strings = ["""{"feilmelding":"Mangler oppgaveId"}""", """{"oppgaveId":null}"""])
    fun `avviser konflikt uten oppgaveId før GET`(konfliktBody: String) {
        stubKonflikt(konfliktBody)

        assertThrows(RuntimeException::class.java) { client.opprettOppgaveM2M(opprettelsesrequest()) }

        verifiserAtIngenOppgaveHentes()
    }

    @ParameterizedTest
    @ValueSource(strings = ["""{"oppgaveId":0}""", """{"oppgaveId":-1}"""])
    fun `avviser konflikt med ikke-positiv oppgaveId før GET`(konfliktBody: String) {
        stubKonflikt(konfliktBody)

        val exception = assertThrows(IllegalStateException::class.java) { client.opprettOppgaveM2M(opprettelsesrequest()) }

        assertEquals("Konfliktsvaret fra Oppgave mangler en gyldig oppgave-ID", exception.message)
        verifiserAtIngenOppgaveHentes()
    }

    @Test
    fun `avviser konflikt med ulesbar body før GET`() {
        stubKonflikt("ikke json")

        val exception = assertThrows(RestClientException::class.java) { client.opprettOppgaveM2M(opprettelsesrequest()) }

        assertInstanceOf(HttpMessageNotReadableException::class.java, exception.cause)
        verifiserAtIngenOppgaveHentes()
    }

    @Test
    fun `avviser konflikt med tom body før GET`() {
        stubKonflikt("")

        val exception = assertThrows(IllegalStateException::class.java) { client.opprettOppgaveM2M(opprettelsesrequest()) }

        assertEquals("Oppgave returnerte et tomt konfliktsvar", exception.message)
        verifiserAtIngenOppgaveHentes()
    }

    private fun stubKonflikt(body: String) {
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/oppgaver"))
                .willReturn(jsonResponse(body).withStatus(409)),
        )
    }

    private fun verifiserAtIngenOppgaveHentes() {
        wireMockServer.verify(0, getRequestedFor(urlPathMatching("/api/v1/oppgaver/.*")))
    }

    private fun stubDuplikatsvar() {
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/oppgaver"))
                .willReturn(
                    jsonResponse(
                        """{"uuid":"${UUID.randomUUID()}","oppgaveId":123,"feilmelding":"Avvist av duplikatkontroll, det finnes allerede en oppgave med uuid $idempotensUuid"}""",
                    ).withStatus(409),
                ),
        )
    }

    private fun stubOppgaveForDuplikat(saksreferanse: String) {
        wireMockServer.stubFor(
            get(urlEqualTo("/api/v1/oppgaver/123"))
                .willReturn(
                    jsonResponse(
                        """{"id":123,"tema":"GRU","oppgavetype":"BEH_SAK","saksreferanse":"$saksreferanse","behandlesAvApplikasjon":"grunn-og-hjelpestonad"}""",
                    ),
                ),
        )
    }

    private fun opprettelsesrequest() =
        LagOppgaveRequest(
            personident = "12345678901",
            saksreferanse = "sak-1",
            tema = Tema.GRU,
            behandlingstema = "ab0132",
            behandlingstype = null,
            fristFerdigstillelse = "2026-09-09",
            aktivDato = "2026-09-08",
            oppgavetype = OppgavetypeGRU.BEH_SAK,
            beskrivelse = "Beskrivelse",
            behandlesAvApplikasjon = "grunn-og-hjelpestonad",
            tildeltEnhetsnr = "1234",
            uuid = idempotensUuid,
        )

    @Test
    fun `henter oppgave med GET`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/api/v1/oppgaver/123"))
                .willReturn(jsonResponse(oppgaveResponse())),
        )

        assertEquals(123, client.hentOppgaveM2M(123).id)
        assertEquals(listOf(oppgaveScope.toString()), texasClient.requestedMaskinAudiences)
    }

    @Test
    fun `oppdaterer oppgave med PATCH`() {
        wireMockServer.stubFor(
            patch(urlEqualTo("/api/v1/oppgaver/123"))
                .willReturn(jsonResponse(oppgaveResponse())),
        )

        assertEquals(123, client.fordelOppgave(oppgaveId = 123, saksbehandler = "Z123456", versjon = 1))
        assertEquals(listOf(oppgaveScope.toString()), texasClient.requestedMaskinAudiences)
    }

    @Test
    fun `kaster exception ved tom respons`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/api/v1/oppgaver/123"))
                .willReturn(aResponse().withStatus(204)),
        )

        val exception =
            assertThrows(NoSuchElementException::class.java) {
                client.hentOppgaveM2M(123)
            }

        assertEquals("Tom respons fra oppgave", exception.message)
    }

    private fun jsonResponse(body: String) =
        aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)

    private fun oppgaveResponse() = """{"id":123,"tema":"GRU","oppgavetype":"BEH_SAK"}"""
}
