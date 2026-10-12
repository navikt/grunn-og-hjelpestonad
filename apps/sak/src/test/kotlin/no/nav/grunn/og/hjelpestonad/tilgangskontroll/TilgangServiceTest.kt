package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

class TilgangServiceTest {
    private val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()

    private val tilgangService =
        TilgangService(
            tilgangsmaskinClient = tilgangsmaskinClient,
            fagsakPersonService = fagsakPersonService,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
        )

    private val ident = "12345678901"

    @BeforeEach
    fun setup() {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .claim("NAVident", "Z999999")
                .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `tilgang når tilgangsmaskinen svarer harTilgang true`() {
        svar(Tilgangsvurdering(personident = ident, harTilgang = true, avvisningskode = null))

        assertDoesNotThrow { tilgangService.validerTilgangTilPerson(ident) }
    }

    @Test
    fun `avslag gir meldingen til avvisningskoden uten fnr`() {
        svar(Tilgangsvurdering(personident = ident, harTilgang = false, avvisningskode = Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE))

        val feil = assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPerson(ident) }

        assertEquals(Avvisningskode.AVVIST_STRENGT_FORTROLIG_ADRESSE.feilmelding, feil.melding)
        assertFalse(feil.melding.contains(ident))
    }

    @Test
    fun `avslag når svaret ikke inneholder identen`() {
        every { tilgangsmaskinClient.sjekkTilgang(any(), any()) } returns emptyList()

        assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPerson(ident) }
    }

    @Test
    fun `avslag når svaret gjelder en annen ident`() {
        svar(Tilgangsvurdering(personident = "10987654321", harTilgang = true, avvisningskode = null))

        assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilPerson(ident) }
    }

    @Test
    fun `TilgangsmaskinException slippes videre`() {
        every { tilgangsmaskinClient.sjekkTilgang(any(), any()) } throws TilgangsmaskinException("Feil ved kall mot tilgangsmaskinen: HTTP 503")

        assertThrows<TilgangsmaskinException> { tilgangService.validerTilgangTilPerson(ident) }
    }

    @Test
    fun `sjekker alltid med KJERNE_REGELTYPE`() {
        val regeltype = slot<Regeltype>()
        every { tilgangsmaskinClient.sjekkTilgang(any(), capture(regeltype)) } returns
            listOf(Tilgangsvurdering(personident = ident, harTilgang = true, avvisningskode = null))

        tilgangService.validerTilgangTilPerson(ident)

        assertEquals(Regeltype.KJERNE_REGELTYPE, regeltype.captured)
    }

    @Test
    fun `fagsakPersonId slås opp til aktiv ident`() {
        val fagsakPersonId = UUID.randomUUID()
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns ident
        svar(Tilgangsvurdering(personident = ident, harTilgang = false, avvisningskode = null))

        assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId) }
    }

    @Test
    fun `fagsakId slås opp til aktiv ident`() {
        val fagsakId = UUID.randomUUID()
        every { fagsakService.hentAktivIdent(fagsakId) } returns ident
        svar(Tilgangsvurdering(personident = ident, harTilgang = false, avvisningskode = null))

        assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilFagsak(fagsakId) }
    }

    @Test
    fun `behandlingId slås opp til aktiv ident via fagsak`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns
            Behandling(id = behandlingId, fagsakId = fagsakId, status = BehandlingStatus.OPPRETTET, resultat = BehandlingResultat.entries.first())
        every { fagsakService.hentAktivIdent(fagsakId) } returns ident
        svar(Tilgangsvurdering(personident = ident, harTilgang = false, avvisningskode = null))

        assertThrows<ManglerTilgangTilPersonFeil> { tilgangService.validerTilgangTilBehandling(behandlingId) }
    }

    @Test
    fun `ukjent behandlingId slipper Feil videre uten å kalle tilgangsmaskinen`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } throws
            Feil(melding = "Fant ikke behandling med id=$behandlingId", httpStatus = HttpStatus.NOT_FOUND)

        val feil = assertThrows<Feil> { tilgangService.validerTilgangTilBehandling(behandlingId) }

        assertEquals(HttpStatus.NOT_FOUND, feil.httpStatus)
        verify(exactly = 0) { tilgangsmaskinClient.sjekkTilgang(any(), any()) }
    }

    private fun svar(vurdering: Tilgangsvurdering) {
        every { tilgangsmaskinClient.sjekkTilgang(setOf(ident), any()) } returns listOf(vurdering)
    }
}
