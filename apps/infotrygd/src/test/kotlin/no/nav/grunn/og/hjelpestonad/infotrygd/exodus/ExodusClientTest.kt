package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExodusClientTest {
    private val properties = ExodusProperties(baseUrl = "http://exodus", scope = "api://exodus/.default")
    private val restClientBuilder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()

    private val texasClient =
        object : TexasClient("http://texas", RestClient.builder()) {
            override fun hentMaskinToken(scope: String) = "token-for-$scope"
        }

    private val client = ExodusClient(properties, texasClient, restClientBuilder)

    @Test
    fun `hentUttrekk sender tabell og iterator og slår sammen skjema og rader`() {
        server
            .expect(requestTo("http://exodus/api/hentUttrekk"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer token-for-api://exodus/.default"))
            .andExpect(content().json("""{"tabellnavn":"t_vedtak","iterator":"forrige","antallRader":2}"""))
            .andRespond(
                withSuccess(
                    """
                    {
                      "iterator": "neste",
                      "schema": {"kolonner": [{"navn": "VEDTAK_ID"}, {"navn": "KODE"}]},
                      "innhold": [["1", "GB"], ["2", null]]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val respons = client.hentUttrekk(ExodusTabell.T_VEDTAK, "forrige", 2)

        assertEquals("neste", respons.iterator)
        assertEquals(
            listOf(mapOf("vedtak_id" to "1", "kode" to "GB"), mapOf("vedtak_id" to "2", "kode" to null)),
            respons.tilRader(),
        )
        server.verify()
    }

    @Test
    fun `409 fra Exodus gir NyBaselineException`() {
        server
            .expect(requestTo("http://exodus/api/hentUttrekk"))
            .andRespond(withStatus(HttpStatus.CONFLICT))

        assertFailsWith<NyBaselineException> { client.hentUttrekk(ExodusTabell.T_STONAD, null, 10) }
    }
}
