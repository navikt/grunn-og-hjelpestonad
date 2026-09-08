package no.nav.grunn.og.hjelpestonad.iverksett.brev

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Distribusjonstype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DokumentInfoResponse
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Fagsystem
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.JournalpostRequest
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class DokumentklienterTest {
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

    private lateinit var dokarkivClient: DokarkivClient
    private lateinit var dokdistClient: DokdistClient
    private lateinit var texasClient: StubTexasClient

    @BeforeEach
    fun setup() {
        val baseUrl = URI.create("http://localhost:${wireMockServer.port()}")
        texasClient = StubTexasClient()
        dokarkivClient =
            DokarkivClient(
                dokarkivUrl = baseUrl,
                dokarkivScope = URI.create("api://dokarkiv/.default"),
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )
        dokdistClient =
            DokdistClient(
                dokdistUrl = baseUrl,
                dokdistScope = URI.create("api://dokdist/.default"),
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )
    }

    @AfterEach
    fun resetServer() {
        wireMockServer.resetAll()
    }

    @Test
    fun `arkiverer dokument med query og maskintoken`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"))
                .withHeader("Authorization", equalTo("Bearer maskin-token"))
                .willReturn(
                    jsonResponse(
                        """
                        {
                            "dokumenter": [{"dokumentInfoId": "dokument-1"}],
                            "journalpostId": "journalpost-1",
                            "journalpostferdigstilt": true
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        val response = dokarkivClient.arkiverDokument(JournalpostRequest(), forsoekFerdigstill = true)

        assertEquals("journalpost-1", response.journalpostId)
        assertEquals(listOf(DokumentInfoResponse("dokument-1")), response.dokumenter)
        assertEquals(listOf("api://dokarkiv/.default"), texasClient.requestedMaskinAudiences)
    }

    @Test
    fun `distribuerer dokument med maskintoken`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/rest/v1/distribuerjournalpost"))
                .withHeader("Authorization", equalTo("Bearer maskin-token"))
                .willReturn(jsonResponse("""{"bestillingsId":"bestilling-1"}""")),
        )

        dokdistClient.distribuerDokument(
            DistribuerJournalpostRequest(
                journalpostId = "journalpost-1",
                bestillendeFagsystem = Fagsystem.EY,
                dokumentProdApp = "grunn-og-hjelpestonad",
                distribusjonstype = Distribusjonstype.VEDTAK,
            ),
        )

        assertEquals(listOf("api://dokdist/.default"), texasClient.requestedMaskinAudiences)
    }

    private fun jsonResponse(body: String) =
        aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
}
