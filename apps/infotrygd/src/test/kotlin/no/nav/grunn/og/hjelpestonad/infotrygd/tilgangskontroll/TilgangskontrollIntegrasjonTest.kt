package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import jakarta.servlet.Filter
import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.security.JwtTestHelper
import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestClient
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import kotlin.test.assertTrue

/**
 * Kjører hele kjeden med ekte security-filter, converter og controllere. Bare JWT-dekodingen og tilgangsmaskinen
 * er byttet ut: tokenverdien i testen peker på et ferdig [Jwt].
 */
@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
@Import(TilgangskontrollIntegrasjonTest.TestOppsett::class)
class TilgangskontrollIntegrasjonTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var tilgangsmaskin: FalskTilgangsmaskin

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun oppsett() {
        val securityFilter = context.getBean("springSecurityFilterChain", Filter::class.java)
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters<DefaultMockMvcBuilder>(securityFilter).build()
        tilgangsmaskin.nullstill()
    }

    @Test
    fun `saksbehandler med tilgang får svar`() {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, true, null) } }

        hentStønader(SAKSBEHANDLER).andExpect { status { isOk() } }
    }

    @Test
    fun `saksbehandler uten tilgang til én person får 403 uten data`() {
        tilgangsmaskin.svar = { listOf(Tilgangsvurdering(PERSON_1, true, null), Tilgangsvurdering(PERSON_2, false, null)) }

        hentStønader(SAKSBEHANDLER, PERSON_1, PERSON_2).andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_FLERE_MELDING) }
            jsonPath("$.stonader") { doesNotExist() }
        }
    }

    @Test
    fun `alle personendepunkter sjekker tilgang`() {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, false, null) } }

        listOf("/api/infotrygd/stonad/eksisterer", "/api/infotrygd/saker/finn").forEach { sti ->
            mockMvc
                .post(sti) {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $SAKSBEHANDLER")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personidenter":["$PERSON_1"]}"""
                }.andExpect { status { isForbidden() } }
        }
    }

    @Test
    fun `stonader sjekker tilgang før data hentes`() {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, false, null) } }

        mockMvc
            .post("/api/infotrygd/stonader") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $SAKSBEHANDLER")
                contentType = MediaType.APPLICATION_JSON
                content = """{"personidenter":["$PERSON_1"],"stønadstype":"GRUNNSTØNAD"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.stonader") { doesNotExist() }
            }
        assertTrue(tilgangsmaskin.kall.single() == setOf(PERSON_1))
    }

    @Test
    fun `feil i tilgangsmaskinen gir 503`() {
        tilgangsmaskin.svar = { throw TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: HTTP 500", 500) }

        hentStønader(SAKSBEHANDLER).andExpect { status { isServiceUnavailable() } }
    }

    @Test
    fun `migreringspersoner med for stor antall gir 400`() {
        mockMvc
            .get("/api/infotrygd/stonad/migreringspersoner?antall=1001") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $SAKSBEHANDLER")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `ugyldig token gir 401`() {
        hentStønader("ukjent-token").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `token uten NAVident, som maskintokens, gir 401`() {
        hentStønader(UTEN_NAVIDENT).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `kall uten token gir 401`() {
        mockMvc
            .post("/api/infotrygd/stonader") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"personidenter":["$PERSON_1"],"stønadstype":"GRUNNSTØNAD"}"""
            }.andExpect { status { isUnauthorized() } }
    }

    private fun hentStønader(
        token: String,
        vararg personidenter: String = arrayOf(PERSON_1),
    ): ResultActionsDsl =
        mockMvc.post("/api/infotrygd/stonader") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"personidenter":[${personidenter.joinToString(",") { "\"$it\"" }}],"stønadstype":"GRUNNSTØNAD"}"""
        }

    class FalskTilgangsmaskin :
        TilgangsmaskinClient(
            URI("http://tilgangsmaskin"),
            "scope",
            object : TexasClient("http://texas", "http://texas/exchange", RestClient.builder()) {},
            RestClient.builder(),
        ) {
        var svar: (Set<String>) -> List<Tilgangsvurdering> = { error("Tilgangsmaskinen skulle ikke vært kalt") }
        val kall = mutableListOf<Set<String>>()

        fun nullstill() {
            svar = { error("Tilgangsmaskinen skulle ikke vært kalt") }
            kall.clear()
        }

        override fun sjekkTilgang(
            personidenter: Set<String>,
            regeltype: Regeltype,
        ): List<Tilgangsvurdering> {
            kall += personidenter
            return svar(personidenter)
        }
    }

    @TestConfiguration
    open class TestOppsett {
        @Bean
        @Primary
        open fun falskTilgangsmaskin() = FalskTilgangsmaskin()

        @Bean
        open fun jwtDecoder(): JwtDecoder {
            val tokens: Map<String, Jwt> =
                mapOf(
                    SAKSBEHANDLER to JwtTestHelper.opprettSaksbehandlerToken(),
                    UTEN_NAVIDENT to JwtTestHelper.opprettTokenUtenNavIdent(),
                )
            return JwtDecoder { token -> tokens[token] ?: throw BadJwtException("Ukjent testtoken") }
        }
    }

    companion object {
        private const val SAKSBEHANDLER = "saksbehandler"
        private const val UTEN_NAVIDENT = "uten-navident"

        private const val PERSON_1 = "12345678901"
        private const val PERSON_2 = "12345678902"
    }
}
