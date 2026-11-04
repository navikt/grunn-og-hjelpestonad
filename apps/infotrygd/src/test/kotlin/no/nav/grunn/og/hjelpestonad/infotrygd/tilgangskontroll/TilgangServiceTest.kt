package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.grunn.og.hjelpestonad.infotrygd.security.JwtTestHelper
import no.nav.grunn.og.hjelpestonad.infotrygd.security.SperretGruppeIDev
import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TilgangServiceTest {
    private val tilgangsmaskin = FalskTilgangsmaskinClient()
    private val tilgangService = TilgangService(tilgangsmaskin, Auditlogg())
    private val auditAppender = ListAppender<ILoggingEvent>()
    private val auditLogger = LoggerFactory.getLogger("auditLogger") as Logger
    private val navIdent = JwtTestHelper.opprettSaksbehandlerToken().getClaimAsString("NAVident")

    @BeforeEach
    fun oppsett() {
        auditAppender.start()
        auditLogger.addAppender(auditAppender)
    }

    @AfterEach
    fun ryddOpp() {
        SecurityContextHolder.clearContext()
        auditLogger.detachAppender(auditAppender)
    }

    @Test
    fun `tilgang til alle personene gir ingen feil`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG)

        assertEquals(listOf(setOf(PERSON_1, PERSON_2)), tilgangsmaskin.kall)
    }

    @Test
    fun `ett avslag gir 403 for hele forespørselen`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_2, null)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG) }

        assertEquals(TilgangService.MANGLER_TILGANG_FLERE_MELDING, feil.melding)
    }

    @Test
    fun `avslag på én person gir meldingen for avvisningskoden`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(avslag(PERSON_1, Avvisningskode.AVVIST_SKJERMING)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertEquals(Avvisningskode.AVVIST_SKJERMING.feilmelding, feil.melding)
    }

    @Test
    fun `ulike avvisningskoder gir generell melding`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = {
            listOf(avslag(PERSON_1, Avvisningskode.AVVIST_SKJERMING), avslag(PERSON_2, Avvisningskode.AVVIST_FORTROLIG_ADRESSE))
        }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG) }

        assertEquals(TilgangService.MANGLER_TILGANG_FLERE_MELDING, feil.melding)
    }

    @Test
    fun `person som mangler i svaret regnes som avslag`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG) }
    }

    @Test
    fun `svar for en person som ikke ble spurt om gir ikke tilgang`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_2)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }
    }

    @Test
    fun `motstridende svar for samme person regnes som avslag`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_1, null)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }
    }

    @Test
    fun `feilmeldingen inneholder aldri personidenter`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(avslag(PERSON_1, null), avslag(PERSON_2, Avvisningskode.AVVIST_SKJERMING)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG) }

        assertFalse(feil.melding.contains(PERSON_1) || feil.melding.contains(PERSON_2))
    }

    @Test
    fun `feil fra tilgangsmaskinen slipper ikke gjennom`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { throw TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: HTTP 500", 500) }

        assertFailsWith<TilgangsmaskinException> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }
    }

    @Test
    fun `flere enn 1000 personer gir ugyldig forespørsel`() {
        loggInnSaksbehandler()
        val identer = (1..1001).map { it.toString().padStart(11, '0') }.toSet()

        assertFailsWith<IllegalArgumentException> { tilgangService.validerTilgangTilPersoner(identer, OPPSLAG) }
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @Test
    fun `filtrering beholder bare personer med tilgang i samme rekkefølge`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_3), avslag(PERSON_2, null), tilgang(PERSON_1)) }

        val resultat = tilgangService.filtrerPersonerMedTilgang(linkedSetOf(PERSON_3, PERSON_2, PERSON_1))

        assertEquals(listOf(PERSON_3, PERSON_1), resultat.toList())
    }

    @Test
    fun `filtrering fjerner personer tilgangsmaskinen ikke svarer for`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1)) }

        assertEquals(setOf(PERSON_1), tilgangService.filtrerPersonerMedTilgang(setOf(PERSON_1, PERSON_2)))
    }

    @Test
    fun `filtrering av tom liste spør ikke tilgangsmaskinen`() {
        loggInnSaksbehandler()

        assertEquals(emptySet(), tilgangService.filtrerPersonerMedTilgang(emptySet()))
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @Test
    fun `sjekken bruker kjerneregelsettet`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG)

        assertEquals(listOf(Regeltype.KJERNE_REGELTYPE), tilgangsmaskin.regeltyper)
    }

    @Test
    fun `tilgang gir én auditlinje med NAV-ident, begge fnr, hva det spørres om og utfall tilgang`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), Oppslag.HENTE_SAKER)

        assertEquals(
            listOf("$navIdent ba om tilgang til fnr: $PERSON_1, $PERSON_2 for å se saker i Infotrygd. Utfall: tilgang"),
            auditLinjer(),
        )
    }

    @Test
    fun `avslag gir én auditlinje med utfall avslag og ManglerTilgangTilPersonFeil kastes fortsatt`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_2, null)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2), OPPSLAG) }

        assertEquals(listOf("$navIdent ba om tilgang til fnr: $PERSON_1, $PERSON_2 for å ${OPPSLAG.tekst}. Utfall: avslag"), auditLinjer())
    }

    @Test
    fun `person som mangler i svaret gir utfall avslag`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { emptyList() }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertEquals(listOf("$navIdent ba om tilgang til fnr: $PERSON_1 for å ${OPPSLAG.tekst}. Utfall: avslag"), auditLinjer())
    }

    @Test
    fun `feil fra tilgangsmaskinen gir én auditlinje med utfall feil og unntaket kastes videre`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { throw TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: HTTP 500", 500) }

        assertFailsWith<TilgangsmaskinException> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertEquals(listOf("$navIdent ba om tilgang til fnr: $PERSON_1 for å ${OPPSLAG.tekst}. Utfall: feil"), auditLinjer())
    }

    @Test
    fun `filtrering gir ingen auditlinje`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_2, null)) }

        tilgangService.filtrerPersonerMedTilgang(setOf(PERSON_1, PERSON_2))

        assertTrue(auditLinjer().isEmpty())
    }

    @Test
    fun `mer enn maks antall personer gir ingen auditlinje`() {
        loggInnSaksbehandler()
        val identer = (1..TilgangService.MAKS_ANTALL_PERSONER + 1).map { it.toString().padStart(11, '0') }.toSet()

        assertFailsWith<IllegalArgumentException> { tilgangService.validerTilgangTilPersoner(identer, OPPSLAG) }

        assertTrue(auditLinjer().isEmpty())
    }

    @ParameterizedTest
    @EnumSource(SperretGruppeIDev::class)
    fun `sperret gruppe gir avslag uten kall til tilgangsmaskinen selv om den ville gitt tilgang`(gruppe: SperretGruppeIDev) {
        loggInnSaksbehandlerMedGruppe(gruppe.gruppeId)
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertEquals(TilgangService.SPERRET_GRUPPE_I_DEV_MELDING, feil.melding)
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @Test
    fun `sperret gruppe gir auditlinje med utfall avslag`() {
        loggInnSaksbehandlerMedGruppe(SperretGruppeIDev.FORTROLIG_ADRESSE.gruppeId)

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertEquals(listOf("$navIdent ba om tilgang til fnr: $PERSON_1 for å ${OPPSLAG.tekst}. Utfall: avslag"), auditLinjer())
    }

    @Test
    fun `meldingen ved sperret gruppe inneholder ikke personidenter`() {
        loggInnSaksbehandlerMedGruppe(SperretGruppeIDev.STRENGT_FORTROLIG_ADRESSE.gruppeId)

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }

        assertFalse(feil.melding.contains(PERSON_1))
    }

    @ParameterizedTest
    @EnumSource(SperretGruppeIDev::class)
    fun `filtrering med sperret gruppe gir ingen personer uten kall til tilgangsmaskinen`(gruppe: SperretGruppeIDev) {
        loggInnSaksbehandlerMedGruppe(gruppe.gruppeId)
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        assertEquals(emptySet(), tilgangService.filtrerPersonerMedTilgang(setOf(PERSON_1, PERSON_2)))
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    @Test
    fun `ukjente grupper ved siden av saksbehandlergruppen sperrer ikke`() {
        loggInnSaksbehandlerMedGruppe("00000000-0000-0000-0000-000000000000")
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG)

        assertEquals(listOf(setOf(PERSON_1)), tilgangsmaskin.kall)
    }

    @Test
    fun `token uten groups gir feil og ikke tilgang`() {
        loggInn(JwtTestHelper.opprettTokenUtenGrupper())
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        assertFailsWith<IllegalStateException> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }
        assertFailsWith<IllegalStateException> { tilgangService.filtrerPersonerMedTilgang(setOf(PERSON_1)) }
        assertTrue(tilgangsmaskin.kall.isEmpty())
        assertEquals(listOf("$navIdent ba om tilgang til fnr: $PERSON_1 for å ${OPPSLAG.tekst}. Utfall: feil"), auditLinjer())
    }

    @Test
    fun `token med overage gir feil og ikke tilgang`() {
        val saksbehandlerToken = JwtTestHelper.opprettSaksbehandlerToken()
        loggInn(
            Jwt
                .withTokenValue("overage")
                .headers { it.putAll(saksbehandlerToken.headers) }
                .claims { it.putAll(saksbehandlerToken.claims) }
                .claim("_claim_names", mapOf("groups" to "src1"))
                .build(),
        )
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        assertFailsWith<IllegalStateException> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1), OPPSLAG) }
        assertFailsWith<IllegalStateException> { tilgangService.filtrerPersonerMedTilgang(setOf(PERSON_1)) }
        assertTrue(tilgangsmaskin.kall.isEmpty())
    }

    private fun auditLinjer(): List<String> = auditAppender.list.map { it.formattedMessage }

    private fun loggInnSaksbehandler() = loggInn(JwtTestHelper.opprettSaksbehandlerToken())

    private fun loggInnSaksbehandlerMedGruppe(gruppeId: String) {
        val saksbehandlerGrupper = JwtTestHelper.opprettSaksbehandlerToken().getClaimAsStringList("groups").orEmpty()
        loggInn(JwtTestHelper.opprettGyldigToken(azureGrupper = saksbehandlerGrupper + gruppeId))
    }

    private fun loggInn(jwt: Jwt) {
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_SAKSBEHANDLER")))
    }

    private fun tilgang(personident: String) = Tilgangsvurdering(personident, harTilgang = true, avvisningskode = null)

    private fun avslag(
        personident: String,
        avvisningskode: Avvisningskode?,
    ) = Tilgangsvurdering(personident, harTilgang = false, avvisningskode = avvisningskode, traceId = "trace-$personident")

    private class FalskTilgangsmaskinClient :
        TilgangsmaskinClient(
            URI("http://tilgangsmaskin"),
            "scope",
            object : TexasClient("http://texas", "http://texas/exchange", RestClient.builder()) {},
            RestClient.builder(),
        ) {
        var svar: (Set<String>) -> List<Tilgangsvurdering> = { error("Tilgangsmaskinen skulle ikke vært kalt") }
        val kall = mutableListOf<Set<String>>()
        val regeltyper = mutableListOf<Regeltype>()

        override fun sjekkTilgang(
            personidenter: Set<String>,
            regeltype: Regeltype,
        ): List<Tilgangsvurdering> {
            kall += personidenter
            regeltyper += regeltype
            return svar(personidenter)
        }
    }

    companion object {
        private const val PERSON_1 = "12345678901"
        private const val PERSON_2 = "12345678902"
        private const val PERSON_3 = "12345678903"
        private val OPPSLAG = Oppslag.HENTE_GRUNNSTØNADER
    }
}
