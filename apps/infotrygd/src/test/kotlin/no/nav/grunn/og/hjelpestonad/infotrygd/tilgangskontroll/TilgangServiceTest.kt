package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import no.nav.grunn.og.hjelpestonad.infotrygd.security.JwtTestHelper
import no.nav.grunn.og.hjelpestonad.infotrygd.texas.TexasClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TilgangServiceTest {
    private val tilgangsmaskin = FalskTilgangsmaskinClient()
    private val tilgangService = TilgangService(tilgangsmaskin)

    @AfterEach
    fun ryddOpp() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `tilgang til alle personene gir ingen feil`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { identer -> identer.map { tilgang(it) } }

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2))

        assertEquals(listOf(setOf(PERSON_1, PERSON_2)), tilgangsmaskin.kall)
    }

    @Test
    fun `ett avslag gir 403 for hele forespørselen`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_2, null)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2)) }

        assertEquals(TilgangService.MANGLER_TILGANG_FLERE_MELDING, feil.melding)
    }

    @Test
    fun `avslag på én person gir meldingen for avvisningskoden`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(avslag(PERSON_1, Avvisningskode.AVVIST_SKJERMING)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1)) }

        assertEquals(Avvisningskode.AVVIST_SKJERMING.feilmelding, feil.melding)
    }

    @Test
    fun `ulike avvisningskoder gir generell melding`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = {
            listOf(avslag(PERSON_1, Avvisningskode.AVVIST_SKJERMING), avslag(PERSON_2, Avvisningskode.AVVIST_FORTROLIG_ADRESSE))
        }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2)) }

        assertEquals(TilgangService.MANGLER_TILGANG_FLERE_MELDING, feil.melding)
    }

    @Test
    fun `person som mangler i svaret regnes som avslag`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2)) }
    }

    @Test
    fun `svar for en person som ikke ble spurt om gir ikke tilgang`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_2)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1)) }
    }

    @Test
    fun `motstridende svar for samme person regnes som avslag`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(tilgang(PERSON_1), avslag(PERSON_1, null)) }

        assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1)) }
    }

    @Test
    fun `feilmeldingen inneholder aldri personidenter`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { listOf(avslag(PERSON_1, null), avslag(PERSON_2, Avvisningskode.AVVIST_SKJERMING)) }

        val feil = assertFailsWith<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1, PERSON_2)) }

        assertFalse(feil.melding.contains(PERSON_1) || feil.melding.contains(PERSON_2))
    }

    @Test
    fun `feil fra tilgangsmaskinen slipper ikke gjennom`() {
        loggInnSaksbehandler()
        tilgangsmaskin.svar = { throw TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: HTTP 500", 500) }

        assertFailsWith<TilgangsmaskinException> { tilgangService.validerTilgangTilPersoner(setOf(PERSON_1)) }
    }

    @Test
    fun `flere enn 1000 personer gir ugyldig forespørsel`() {
        loggInnSaksbehandler()
        val identer = (1..1001).map { it.toString().padStart(11, '0') }.toSet()

        assertFailsWith<IllegalArgumentException> { tilgangService.validerTilgangTilPersoner(identer) }
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

        tilgangService.validerTilgangTilPersoner(setOf(PERSON_1))

        assertEquals(listOf(Regeltype.KJERNE_REGELTYPE), tilgangsmaskin.regeltyper)
    }

    private fun loggInnSaksbehandler() {
        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(JwtTestHelper.opprettSaksbehandlerToken(), listOf(SimpleGrantedAuthority("ROLE_SAKSBEHANDLER")))
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
    }
}
