package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.config.testRestClientBuilder
import no.nav.grunn.og.hjelpestonad.texas.TexasClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI

class TilgangsmaskinClientTest {
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

        private const val IDENT = "12345678901"
        private const val BULK_STI = "/api/v1/bulk/obo"
    }

    private val tilgangsmaskinScope = "api://test/.default"
    private val texasClient = mockk<TexasClient>()
    private lateinit var client: TilgangsmaskinClient

    @BeforeEach
    fun setup() {
        client =
            TilgangsmaskinClient(
                tilgangsmaskinUrl = URI.create("http://localhost:${wireMockServer.port()}"),
                tilgangsmaskinScope = tilgangsmaskinScope,
                texasClient = texasClient,
                restClientBuilder = testRestClientBuilder(),
            )
        every { texasClient.hentOboToken(tilgangsmaskinScope) } returns "gyldig-token"
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
    }

    @Nested
    inner class Request {
        @Test
        fun `sender identer med regeltype og OBO-token til bulk-endepunktet`() {
            stubSvar(resultater = """{"brukerId":"$IDENT","status":204}""")

            runCatching { client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE) }

            wireMockServer.verify(
                postRequestedFor(urlEqualTo(BULK_STI))
                    .withHeader("Authorization", equalTo("Bearer gyldig-token"))
                    .withRequestBody(equalToJson("""[{"brukerId":"$IDENT","type":"KOMPLETT_REGELTYPE"}]""")),
            )
            verify(exactly = 1) { texasClient.hentOboToken(tilgangsmaskinScope) }
        }

        @Test
        fun `tomt sett gir tom liste uten kall`() {
            val resultat = client.sjekkTilgang(setOf("", " "), Regeltype.KOMPLETT_REGELTYPE)

            assertTrue(resultat.isEmpty())
            assertEquals(0, wireMockServer.allServeEvents.size)
            verify(exactly = 0) { texasClient.hentOboToken(any()) }
        }
    }

    @Nested
    inner class Feil {
        @Test
        fun `HTTP-feil gir TilgangsmaskinException uten fnr og responsbody i meldingen`() {
            wireMockServer.stubFor(
                post(urlEqualTo(BULK_STI)).willReturn(
                    aResponse().withStatus(500).withBody("""{"detail":"feil for bruker $IDENT"}"""),
                ),
            )

            val feil = assertThrows<TilgangsmaskinException> { client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE) }

            assertEquals(500, feil.httpStatus)
            assertFalse(feil.message!!.contains(IDENT))
            assertFalse(feil.message!!.contains("detail"))
        }

        @Test
        fun `nettverksfeil gir TilgangsmaskinException`() {
            wireMockServer.stubFor(
                post(urlEqualTo(BULK_STI)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
            )

            assertThrows<TilgangsmaskinException> { client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE) }
        }

        @Test
        fun `feil ved henting av OBO-token gir TilgangsmaskinException`() {
            every { texasClient.hentOboToken(tilgangsmaskinScope) } throws IllegalStateException("ingen token")

            assertThrows<TilgangsmaskinException> { client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE) }
        }
    }

    @Nested
    inner class TolkningAvSvar {
        @Test
        fun `status 204 gir tilgang`() {
            stubSvar(resultater = """{"brukerId":"$IDENT","status":204}""")

            val vurdering = client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE).single()

            assertEquals(IDENT, vurdering.personident)
            assertTrue(vurdering.harTilgang)
        }

        @Test
        fun `status 403 gir avslag med avvisningskode fra title`() {
            stubSvar(
                resultater =
                    """{"brukerId":"$IDENT","status":403,"detaljer":{"title":"AVVIST_SKJERMING","begrunnelse":"x","traceId":"abc","kanOverstyres":false}}""",
            )

            val vurdering = client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE).single()

            assertFalse(vurdering.harTilgang)
            assertEquals(Avvisningskode.AVVIST_SKJERMING, vurdering.avvisningskode)
            assertEquals("abc", vurdering.traceId)
        }

        @Test
        fun `status 404 gir avslag`() {
            stubSvar(resultater = """{"brukerId":"$IDENT","status":404}""")

            assertFalse(client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE).single().harTilgang)
        }

        @Test
        fun `ident som mangler i svaret gir ingen vurdering`() {
            stubSvar(resultater = "")

            val vurderinger = client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE)

            assertTrue(vurderinger.isEmpty())
        }

        @Test
        fun `ukjent avvisningskode gir ingen avvisningskode`() {
            stubSvar(resultater = """{"brukerId":"$IDENT","status":403,"detaljer":{"title":"AVVIST_NY_REGEL"}}""")

            val vurdering = client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE).single()

            assertFalse(vurdering.harTilgang)
            assertNull(vurdering.avvisningskode)
        }

        @Test
        fun `manglende detaljer gir ingen avvisningskode`() {
            stubSvar(resultater = """{"brukerId":"$IDENT","status":403}""")

            assertNull(client.sjekkTilgang(setOf(IDENT), Regeltype.KOMPLETT_REGELTYPE).single().avvisningskode)
        }
    }

    private fun stubSvar(resultater: String) {
        wireMockServer.stubFor(
            post(urlEqualTo(BULK_STI))
                .willReturn(
                    aResponse()
                        .withStatus(207)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"ansattId":"Z123456","resultater":[$resultater]}"""),
                ),
        )
    }
}
