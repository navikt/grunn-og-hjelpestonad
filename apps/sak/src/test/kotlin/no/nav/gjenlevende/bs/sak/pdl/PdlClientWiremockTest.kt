package no.nav.gjenlevende.bs.sak.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.gjenlevende.bs.sak.texas.TexasClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

class PdlClientWiremockTest {
    companion object {
        private lateinit var wireMockServer: WireMockServer
        private lateinit var pdlClient: PdlClient

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
            val texasClient =
                mockk<TexasClient>().apply {
                    every { hentOboToken(any()) } returns "texas-obo-token-xyz"
                }

            pdlClient =
                PdlClient(
                    texasClient = texasClient,
                    pdlScope = "pdlScope",
                    pdlUrl = "http://localhost:${wireMockServer.port()}",
                )
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
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
