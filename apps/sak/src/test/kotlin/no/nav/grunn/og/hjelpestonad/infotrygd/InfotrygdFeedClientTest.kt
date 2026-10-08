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
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClientResponseException
import java.time.LocalDate
import kotlin.test.assertFailsWith

class InfotrygdFeedClientTest {
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
    private lateinit var client: InfotrygdFeedClient

    @BeforeEach
    fun setup() {
        client =
            InfotrygdFeedClient(
                texasClient = texasClient,
                infotrygdFeedUrl = "http://localhost:${wireMockServer.port()}",
                infotrygdFeedScope = AUDIENCE,
                restClientBuilder = testRestClientBuilder(),
            )
        every { texasClient.hentMaskinToken(AUDIENCE) } returns "gyldig-token"
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    @Test
    fun `sender startbehandling med maskintoken`() {
        // Arrange
        wireMockServer.stubFor(
            post(urlEqualTo("/api/grunnstonad/v1/feed/startbehandlingsmelding"))
                .withRequestBody(equalToJson("""{"personIdent":"12345678901"}"""))
                .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                .willReturn(aResponse().withStatus(204)),
        )

        // Act
        client.sendStartBehandling("12345678901")

        // Assert
        verify(exactly = 1) { texasClient.hentMaskinToken(AUDIENCE) }
    }

    @Test
    fun `sender vedtak med første stønadsdato`() {
        // Arrange
        wireMockServer.stubFor(
            post(urlEqualTo("/api/grunnstonad/v1/feed/vedtaksmelding"))
                .withRequestBody(equalToJson("""{"personIdent":"12345678901","datoStartNyGrunnstønad":"2026-01-01"}"""))
                .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                .willReturn(aResponse().withStatus(204)),
        )

        // Act
        client.sendVedtak("12345678901", LocalDate.of(2026, 1, 1))

        // Assert
        verify(exactly = 1) { texasClient.hentMaskinToken(AUDIENCE) }
    }

    @Test
    fun `kaster exception når feeden returnerer feilstatus`() {
        // Arrange
        wireMockServer.stubFor(
            post(urlEqualTo("/api/grunnstonad/v1/feed/startbehandlingsmelding"))
                .willReturn(aResponse().withStatus(500)),
        )

        // Act & Assert
        assertFailsWith<RestClientResponseException> { client.sendStartBehandling("12345678901") }
    }
}
