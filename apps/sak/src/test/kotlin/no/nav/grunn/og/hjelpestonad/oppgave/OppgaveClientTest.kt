package no.nav.grunn.og.hjelpestonad.oppgave

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

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
                .willReturn(jsonResponse(oppgaveResponse())),
        )

        val response =
            client.opprettOppgaveM2M(
                LagOppgaveRequest(
                    personident = "12345678901",
                    saksreferanse = "sak-1",
                    tema = Tema.EYO,
                    fristFerdigstillelse = "2026-09-09",
                    aktivDato = "2026-09-08",
                    oppgavetype = OppgavetypeEYO.BEH_SAK,
                    beskrivelse = "Beskrivelse",
                    behandlesAvApplikasjon = "grunn-og-hjelpestonad",
                    tildeltEnhetsnr = "1234",
                ),
            )

        assertEquals(123, response.id)
        assertEquals(listOf(oppgaveScope.toString()), texasClient.requestedMaskinAudiences)
    }

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

    private fun oppgaveResponse() = """{"id":123,"tema":"EYO","oppgavetype":"BEH_SAK"}"""
}
