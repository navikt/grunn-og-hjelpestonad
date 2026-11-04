package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.Filter
import no.nav.grunn.og.hjelpestonad.infotrygd.Application
import no.nav.grunn.og.hjelpestonad.infotrygd.security.JwtTestHelper
import no.nav.grunn.og.hjelpestonad.infotrygd.security.SperretGruppeIDev
import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
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
    fun `saksbehandler uten tilgang til personen får 403 uten data`() {
        tilgangsmaskin.svar = { listOf(Tilgangsvurdering(PERSON_1, false, null)) }

        hentStønader(SAKSBEHANDLER, PERSON_1).andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.MANGLER_TILGANG_MELDING) }
            jsonPath("$.stønader") { doesNotExist() }
        }
    }

    @Test
    fun `flere personidenter gir 400 uten kall til tilgangsmaskinen`() {
        val personidenter = """["$PERSON_1","$PERSON_2"]"""
        mapOf(
            "/api/infotrygd/stonader" to """{"personidenter":$personidenter,"stønadstype":"GRUNNSTØNAD"}""",
            "/api/infotrygd/stonad/eksisterer" to """{"personidenter":$personidenter}""",
            "/api/infotrygd/saker/finn" to """{"personidenter":$personidenter}""",
        ).forEach { (sti, body) ->
            mockMvc
                .post(sti) {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $SAKSBEHANDLER")
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.melding") { value("Kan bare slå opp én personident om gangen") }
                }
        }
        assertTrue(tilgangsmaskin.kall.isEmpty())
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
                jsonPath("$.stønader") { doesNotExist() }
            }
        assertTrue(tilgangsmaskin.kall.single() == setOf(PERSON_1))
    }

    @Test
    fun `auditlinjen for stonader sier hvilken stønadstype saksbehandleren spør om`() {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, false, null) } }
        val auditAppender = ListAppender<ILoggingEvent>().apply { start() }
        val auditLogger = LoggerFactory.getLogger("auditLogger") as Logger
        auditLogger.addAppender(auditAppender)

        try {
            mockMvc
                .post("/api/infotrygd/stonader") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $SAKSBEHANDLER")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personidenter":["$PERSON_1"],"stønadstype":"HJELPESTØNAD"}"""
                }.andExpect { status { isForbidden() } }
        } finally {
            auditLogger.detachAppender(auditAppender)
        }

        val linje = auditAppender.list.single().formattedMessage
        assertTrue(linje.contains("for å ${Oppslag.HENTE_HJELPESTØNADER.tekst}. Utfall: avslag"), linje)
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

    @ParameterizedTest(name = "sperret saksbehandler med {0} får 403 før tilgangsmaskinen spørres")
    @EnumSource(SperretGruppeIDev::class)
    fun `sperret saksbehandler får 403 uten data og uten kall til tilgangsmaskinen`(sperret: SperretGruppeIDev) {
        // Tilgangsmaskinen ville gitt tilgang. Testen beviser at sperren slår til før den spørres.
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, true, null) } }

        hentStønader(sperretSaksbehandlerToken(sperret)).andExpect {
            status { isForbidden() }
            jsonPath("$.melding") { value(TilgangService.SPERRET_GRUPPE_I_DEV_MELDING) }
            jsonPath("$.stønader") { doesNotExist() }
        }
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @ParameterizedTest(name = "sperret saksbehandler med {0} får 403 på eksisterer og saker/finn")
    @EnumSource(SperretGruppeIDev::class)
    fun `sperret saksbehandler får 403 på eksisterer og saker finn uten kall til tilgangsmaskinen`(sperret: SperretGruppeIDev) {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, true, null) } }

        listOf("/api/infotrygd/stonad/eksisterer", "/api/infotrygd/saker/finn").forEach { sti ->
            mockMvc
                .post(sti) {
                    header(HttpHeaders.AUTHORIZATION, "Bearer ${sperretSaksbehandlerToken(sperret)}")
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"personidenter":["$PERSON_1"]}"""
                }.andExpect {
                    status { isForbidden() }
                    jsonPath("$.melding") { value(TilgangService.SPERRET_GRUPPE_I_DEV_MELDING) }
                }
        }
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @ParameterizedTest(name = "sperret saksbehandler med {0} får ingen migreringspersoner")
    @EnumSource(SperretGruppeIDev::class)
    fun `sperret saksbehandler får ingen migreringspersoner uten kall til tilgangsmaskinen`(sperret: SperretGruppeIDev) {
        tilgangsmaskin.svar = { identer -> identer.map { Tilgangsvurdering(it, true, null) } }

        // Testdatabasen er tom her, så finnPersonerForMigrering gir ingen kandidater og filteret har ingenting å fjerne.
        // Testen viser derfor at svaret er tomt og at tilgangsmaskinen ikke kalles. Den viser ikke at filteret virker på
        // ekte kandidater.
        mockMvc
            .get("/api/infotrygd/stonad/migreringspersoner?antall=10") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${sperretSaksbehandlerToken(sperret)}")
            }.andExpect {
                status { isOk() }
                jsonPath("$.personidenter") { isEmpty() }
            }
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @ParameterizedTest(name = "token med bare {0} og ingen rollegruppe gir 401")
    @EnumSource(SperretGruppeIDev::class)
    fun `token med bare en sperret gruppe og ingen rollegruppe gir 401`(sperret: SperretGruppeIDev) {
        hentStønader(bareSperretToken(sperret)).andExpect { status { isUnauthorized() } }
        assertTrue(tilgangsmaskin.kall.isEmpty())
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
                buildMap {
                    put(SAKSBEHANDLER, JwtTestHelper.opprettSaksbehandlerToken())
                    put(UTEN_NAVIDENT, JwtTestHelper.opprettTokenUtenNavIdent())
                    SperretGruppeIDev.entries.forEach { sperret ->
                        put(
                            sperretSaksbehandlerToken(sperret),
                            JwtTestHelper.opprettGyldigToken(azureGrupper = listOf(SAKSBEHANDLER_GRUPPE, sperret.gruppeId)),
                        )
                        put(
                            bareSperretToken(sperret),
                            JwtTestHelper.opprettGyldigToken(azureGrupper = listOf(sperret.gruppeId)),
                        )
                    }
                }
            return JwtDecoder { token -> tokens[token] ?: throw BadJwtException("Ukjent testtoken") }
        }
    }

    companion object {
        private const val SAKSBEHANDLER = "saksbehandler"
        private const val UTEN_NAVIDENT = "uten-navident"
        private const val SAKSBEHANDLER_GRUPPE = "7ce9d1d2-d149-4324-832b-8d459762a102"

        private fun sperretSaksbehandlerToken(sperret: SperretGruppeIDev) = "sperret-${sperret.name}"

        private fun bareSperretToken(sperret: SperretGruppeIDev) = "bare-sperret-${sperret.name}"

        private const val PERSON_1 = "12345678901"
        private const val PERSON_2 = "12345678902"
    }
}
