package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TilgangsmaskinClientTest {
    private val tilgangsmaskinScope = "api://tilgangsmaskin/.default"
    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val texasClient =
        object : TexasClient("http://texas", "http://texas/exchange", RestClient.builder()) {
            override fun hentOboToken(target: String) = "obo-token-for-$target"
        }
    private val client =
        TilgangsmaskinClient(
            tilgangsmaskinUrl = URI.create("http://tilgangsmaskin"),
            tilgangsmaskinScope = tilgangsmaskinScope,
            texasClient = texasClient,
            restClientBuilder = restClientBuilder,
        )

    @BeforeEach
    fun setup() {
        server.reset()
    }

    @Test
    fun `sender identer med regeltype og OBO-token til bulk-endepunktet`() {
        val ident = "12345678910"
        server
            .expect(requestTo("http://tilgangsmaskin/api/v1/bulk/obo"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer obo-token-for-$tilgangsmaskinScope"))
            .andExpect(
                content().json(
                    """[{"brukerId":"$ident","type":"KJERNE_REGELTYPE"}]""",
                    JsonCompareMode.STRICT,
                ),
            ).andRespond(withSuccess("""{"resultater":[]}""", MediaType.APPLICATION_JSON))

        client.sjekkTilgang(setOf(ident), Regeltype.KJERNE_REGELTYPE)

        server.verify()
    }

    @Test
    fun `blandet svar gir tilgang og avslag med avvisningskode og traceId`() {
        val identMedTilgang = "12345678910"
        val identMedAvslag = "10987654321"
        server
            .expect(requestTo("http://tilgangsmaskin/api/v1/bulk/obo"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "resultater": [
                        {"brukerId":"$identMedTilgang","status":204},
                        {"brukerId":"$identMedAvslag","status":403,"detaljer":{"title":"AVVIST_SKJERMING","traceId":"trace-123"}}
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val vurderinger = client.sjekkTilgang(setOf(identMedTilgang, identMedAvslag), Regeltype.KJERNE_REGELTYPE)

        assertEquals(true, vurderinger.single { it.personident == identMedTilgang }.harTilgang)
        val avslag = vurderinger.single { it.personident == identMedAvslag }
        assertFalse(avslag.harTilgang)
        assertEquals(Avvisningskode.AVVIST_SKJERMING, avslag.avvisningskode)
        assertEquals("trace-123", avslag.traceId)
        server.verify()
    }

    @Test
    fun `ukjent avvisningstittel gir null avvisningskode`() {
        val ident = "12345678910"
        server
            .expect(requestTo("http://tilgangsmaskin/api/v1/bulk/obo"))
            .andRespond(
                withSuccess(
                    """{"resultater":[{"brukerId":"$ident","status":403,"detaljer":{"title":"UKJENT_TITTEL"}}]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val vurdering = client.sjekkTilgang(setOf(ident), Regeltype.KJERNE_REGELTYPE).single()

        assertNull(vurdering.avvisningskode)
        server.verify()
    }

    @Test
    fun `HTTP 500 gir melding uten sensitive opplysninger og beholder cause for Team Logs`() {
        val ident = "12345678910"
        server
            .expect(requestTo("http://tilgangsmaskin/api/v1/bulk/obo"))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"detail":"feil for bruker $ident"}"""),
            )

        val feil =
            assertFailsWith<TilgangsmaskinException> {
                client.sjekkTilgang(setOf(ident), Regeltype.KJERNE_REGELTYPE)
            }

        assertFalse(feil.message!!.contains(ident))
        assertTrue(feil.cause!!.message!!.contains(ident))
        assertEquals(500, feil.httpStatus)
        server.verify()
    }

    @Test
    fun `tomt sett gir tom liste uten HTTP-kall`() {
        val vurderinger = client.sjekkTilgang(setOf("", " "), Regeltype.KJERNE_REGELTYPE)

        assertTrue(vurderinger.isEmpty())
        server.verify()
    }
}
