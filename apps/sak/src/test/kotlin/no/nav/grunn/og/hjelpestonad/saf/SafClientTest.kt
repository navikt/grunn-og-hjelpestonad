package no.nav.grunn.og.hjelpestonad.saf

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.SafConfig
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class SafClientTest {
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

    private lateinit var client: SafClient
    private lateinit var texasClient: StubTexasClient

    @BeforeEach
    fun setup() {
        texasClient = StubTexasClient()
        client =
            SafClient(
                safConfig =
                    SafConfig(
                        safBaseUri = URI.create("http://localhost:${wireMockServer.port()}"),
                        safScope = "api://saf/.default",
                    ),
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )
    }

    @AfterEach
    fun resetServer() {
        wireMockServer.resetAll()
    }

    @Test
    fun `henter journalposter med OBO-token`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .withHeader("Authorization", equalTo("Bearer obo-token"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"data":{"dokumentoversiktBruker":{"journalposter":[]}}}"""),
                ),
        )

        val response =
            client.hentSafJournalpostBrukerData(
                JournalposterForBrukerRequest(
                    brukerId = Bruker(id = "12345678901", type = BrukerIdType.FNR),
                    tema = listOf(Arkivtema.GRU),
                    journalposttype = listOf(Journalposttype.I),
                    antall = 10,
                ),
            )

        assertTrue(response.dokumentoversiktBruker.journalposter.isNullOrEmpty())
        assertEquals(listOf("api://saf/.default"), texasClient.requestedOboAudiences)
    }

    @Test
    fun `henter dokument med OBO-token`() {
        val dokument = "%PDF-test".encodeToByteArray()
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/hentdokument/1/2/ARKIV"))
                .withHeader("Authorization", equalTo("Bearer obo-token"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(dokument),
                ),
        )

        assertArrayEquals(dokument, client.hentDokument(journalpostId = "1", dokumentInfoId = "2"))
        assertEquals(listOf("api://saf/.default"), texasClient.requestedOboAudiences)
    }
}
