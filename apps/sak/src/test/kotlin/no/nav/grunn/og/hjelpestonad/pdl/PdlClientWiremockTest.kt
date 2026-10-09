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
import java.time.LocalDateTime

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

    @Test
    fun `hentFamilieRelasjoner returnerer data ved gyldig respons`() {
        stubForGraphql(jacksonObjectMapper().writeValueAsString(familieRelasjon))
        val familieRelasjonerResponse =
            pdlClient.hentFamilieRelasjoner(
                PdlRequest(query = "query {}", variables = mapOf("ident" to "123")),
            )

        assertThat(familieRelasjonerResponse).isNotNull
        assertThat(
            familieRelasjonerResponse
                ?.hentPerson
                ?.forelderBarnRelasjon
                ?.singleOrNull()
                ?.relatertPersonsIdent,
        ).isEqualTo("456")
        assertThat(
            familieRelasjonerResponse
                ?.hentPerson
                ?.forelderBarnRelasjon
                ?.singleOrNull()
                ?.relatertPersonsRolle,
        ).isEqualTo(Familierolle.BARN)
        assertThat(
            familieRelasjonerResponse
                ?.hentPerson
                ?.forelderBarnRelasjon
                ?.singleOrNull()
                ?.minRolleForPerson,
        ).isEqualTo(Familierolle.MOR)
        assertEquals(listOf("pdlScope"), texasClient.requestedOboAudiences)
    }

    @Test
    fun `hentFamilieRelasjoner kaster PdlException ved teknisk feil`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(serverError()),
        )

        assertThrows<PdlException> {
            pdlClient.hentFamilieRelasjoner(
                PdlRequest(query = "query {}", variables = emptyMap()),
            )
        }
    }

    @Test
    fun `hentFaktagrunnlag leser personopplysningene fra PDL`() {
        stubForGraphql(
            """
            {
              "data": {
                "hentPerson": {
                  "folkeregisterpersonstatus": [
                    {
                      "status": "bosatt",
                      "forenkletStatus": "bosattEtterFolkeregisterloven",
                      "metadata": { "historisk": false, "master": "FREG", "endringer": [] },
                      "folkeregistermetadata": { "gyldighetstidspunkt": "2020-01-01T12:30:00", "opphoerstidspunkt": null }
                    }
                  ],
                  "bostedsadresse": [
                    {
                      "gyldigFraOgMed": "2020-01-01T00:00",
                      "gyldigTilOgMed": null,
                      "angittFlyttedato": "2020-01-01",
                      "vegadresse": null,
                      "matrikkeladresse": null,
                      "utenlandskAdresse": { "landkode": "SWE" },
                      "ukjentBosted": null,
                      "metadata": { "historisk": true, "master": "FREG" },
                      "folkeregistermetadata": null
                    }
                  ],
                  "statsborgerskap": [
                    {
                      "land": "NOR",
                      "gyldigFraOgMed": "1990-01-15",
                      "gyldigTilOgMed": null,
                      "bekreftelsesdato": null,
                      "metadata": { "historisk": false, "master": "FREG" },
                      "folkeregistermetadata": null
                    }
                  ],
                  "opphold": [
                    {
                      "type": "PERMANENT",
                      "oppholdFra": "2019-01-01",
                      "oppholdTil": null,
                      "metadata": { "historisk": false, "master": "FREG" },
                      "folkeregistermetadata": { "gyldighetstidspunkt": null, "opphoerstidspunkt": null }
                    }
                  ],
                  "innflyttingTilNorge": [],
                  "utflyttingFraNorge": [],
                  "oppholdsadresse": [
                    {
                      "gyldigFraOgMed": "2022-03-01T00:00",
                      "gyldigTilOgMed": null,
                      "utenlandskAdresse": { "landkode": "ESP" },
                      "metadata": { "historisk": false, "master": "FREG" },
                      "folkeregistermetadata": null
                    }
                  ],
                  "doedsfall": [
                    {
                      "doedsdato": "2026-09-01",
                      "metadata": { "historisk": false, "master": "FREG" },
                      "folkeregistermetadata": null
                    }
                  ]
                }
              }
            }
            """.trimIndent(),
        )

        val person = pdlClient.hentFaktagrunnlag(PdlRequest(query = "query {}", variables = mapOf("ident" to "123")))

        assertThat(person).isNotNull
        val personstatus = person!!.folkeregisterpersonstatus.single()
        assertThat(personstatus.status).isEqualTo("bosatt")
        assertThat(personstatus.folkeregistermetadata?.gyldighetstidspunkt).isEqualTo(LocalDateTime.of(2020, 1, 1, 12, 30))
        val bostedsadresse = person.bostedsadresse.single()
        assertThat(bostedsadresse.utenlandskAdresse?.landkode).isEqualTo("SWE")
        assertThat(bostedsadresse.gyldigFraOgMed).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0))
        assertThat(bostedsadresse.metadata.historisk).isTrue
        assertThat(person.statsborgerskap.single().land).isEqualTo("NOR")
        assertThat(person.opphold.single().type).isEqualTo("PERMANENT")
        assertThat(person.innflyttingTilNorge).isEmpty()
        assertThat(
            person.oppholdsadresse
                .single()
                .utenlandskAdresse
                ?.landkode,
        ).isEqualTo("ESP")
        assertThat(person.doedsfall.single().doedsdato).isEqualTo(LocalDate.of(2026, 9, 1))
        assertEquals(listOf("pdlScope"), texasClient.requestedOboAudiences)
    }

    @Test
    fun `hentFaktagrunnlag kaster PdlException ved teknisk feil`() {
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(serverError()),
        )

        assertThrows<PdlException> {
            pdlClient.hentFaktagrunnlag(PdlRequest(query = "query {}", variables = emptyMap()))
        }
    }

    private val familieRelasjon =
        PdlResponseFamilierelasjoner(
            data =
                FamilieRelasjonerResponse(
                    hentPerson =
                        FamilieRelasjoner(
                            forelderBarnRelasjon =
                                listOf(
                                    ForelderBarnRelasjon(relatertPersonsIdent = "456", relatertPersonsRolle = Familierolle.BARN, minRolleForPerson = Familierolle.MOR),
                                ),
                        ),
                ),
        )
}

private fun PdlResponseHentPersonData.mapToJsonString() = jacksonObjectMapper().writeValueAsString(this)
