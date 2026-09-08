package no.nav.grunn.og.hjelpestonad.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

class PdlClientWiremockTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer

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

    private lateinit var texasClient: StubTexasClient
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setup() {
        texasClient = StubTexasClient()
        pdlClient =
            PdlClient(
                texasClient = texasClient,
                pdlScope = "pdlScope",
                pdlUrl = "http://localhost:${wireMockServer.port()}",
                restClientBuilder = testRestClientBuilder(),
            )
    }

    @AfterEach
    fun resetServer() {
        wireMockServer.resetAll()
    }

    @Test
    fun `hentPersonData returnerer data ved gyldig respons`() {
        stubForGraphql(lagPdlResponseHentPersonData())
        val result: HentPersonData? =
            pdlClient.hentPersonDataOBOToken(
                PdlRequest(
                    query = "query {}",
                    variables = mapOf("ident" to "123"),
                ),
            )

        assertThat(result).isNotNull
        assertThat(
            result
                ?.hentPerson
                ?.navn
                ?.first()
                ?.fornavn,
        ).isEqualTo("Fornavn")
        assertThat(
            result
                ?.hentPerson
                ?.navn
                ?.first()
                ?.etternavn,
        ).isEqualTo("Etternavn")
        assertEquals(listOf("pdlScope"), texasClient.requestedOboAudiences)
    }

    @Test
    fun `hentPersonData med maskintoken bruker riktig målgruppe`() {
        stubForGraphql(lagPdlResponseHentPersonData())

        val result =
            pdlClient.hentPersonDataMaskinToken(
                PdlRequest(
                    query = "query {}",
                    variables = mapOf("ident" to "123"),
                ),
            )

        assertThat(result).isNotNull
        assertEquals(listOf("pdlScope"), texasClient.requestedMaskinAudiences)
    }

    private fun stubForGraphql(response: String) {
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response),
                ),
        )
    }

    private fun lagPdlResponseHentPersonData(): String {
        val response =
            PdlResponseHentPersonData(
                data =
                    HentPersonData(
                        hentPerson =
                            HentPerson(
                                navn =
                                    listOf(
                                        Navn(
                                            fornavn = "Fornavn",
                                            mellomnavn = null,
                                            etternavn = "Etternavn",
                                        ),
                                    ),
                                foedselsdato = listOf(Foedselsdato(LocalDate.of(1990, 1, 15))),
                            ),
                    ),
                errors = null,
            )
        return response.mapToJsonString()
    }

    @Test
    fun `hentPersonData kaster PdlException ved teknisk feil`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    serverError(),
                ),
        )

        assertThrows<PdlException> {
            pdlClient.hentPersonDataOBOToken(
                PdlRequest(
                    query = "query {}",
                    variables = emptyMap(),
                ),
            )
        }
    }
}

private fun PdlResponseHentPersonData.mapToJsonString() = jacksonObjectMapper().writeValueAsString(this)
