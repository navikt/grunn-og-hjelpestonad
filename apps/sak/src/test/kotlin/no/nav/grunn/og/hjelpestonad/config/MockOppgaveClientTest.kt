package no.nav.grunn.og.hjelpestonad.config

import no.nav.grunn.og.hjelpestonad.oppgave.LagOppgaveRequest
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavePrioritet
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import no.nav.grunn.og.hjelpestonad.oppgave.StatusEnum
import no.nav.grunn.og.hjelpestonad.oppgave.Tema
import no.nav.grunn.og.hjelpestonad.texas.StubTexasClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockOppgaveClientTest {
    private lateinit var mockOppgaveClient: MockOppgaveClient

    @BeforeEach
    fun setup() {
        mockOppgaveClient =
            MockOppgaveClient(
                texasClient = StubTexasClient(),
                restClientBuilder = testRestClientBuilder(),
            )
    }

    @Test
    fun `opprettelse og henting bevarer tildeltEnhetsnr, tilordnetRessurs og oppgavetype`() {
        val opprettetOppgave =
            mockOppgaveClient.opprettOppgaveM2M(lagOppgaveRequest(tilordnetRessurs = "Z990001"))

        val hentetOppgave = mockOppgaveClient.hentOppgaveM2M(checkNotNull(opprettetOppgave.id))

        assertEquals("4420", hentetOppgave.tildeltEnhetsnr)
        assertEquals("Z990001", hentetOppgave.tilordnetRessurs)
        assertEquals(OppgavetypeGRU.BEH_SAK.name, hentetOppgave.oppgavetype)
        assertEquals(1, hentetOppgave.versjon)
        assertEquals(StatusEnum.OPPRETTET, hentetOppgave.status)
    }

    @Test
    fun `fordelOppgave endrer tilordnetRessurs og øker versjon`() {
        val opprettetOppgave = mockOppgaveClient.opprettOppgaveM2M(lagOppgaveRequest())
        val oppgaveId = checkNotNull(opprettetOppgave.id)

        val returnertOppgaveId = mockOppgaveClient.fordelOppgave(oppgaveId, "Z990002", 1)

        val hentetOppgave = mockOppgaveClient.hentOppgaveM2M(oppgaveId)
        assertEquals(oppgaveId, returnertOppgaveId)
        assertEquals("Z990002", hentetOppgave.tilordnetRessurs)
        assertEquals(2, hentetOppgave.versjon)
    }

    @Test
    fun `fjernTilordnetRessurs tømmer tilordnetRessurs og øker versjon`() {
        val opprettetOppgave = mockOppgaveClient.opprettOppgaveM2M(lagOppgaveRequest(tilordnetRessurs = "Z990001"))
        val oppgaveId = checkNotNull(opprettetOppgave.id)

        mockOppgaveClient.fjernTilordnetRessurs(oppgaveId, 1)

        val hentetOppgave = mockOppgaveClient.hentOppgaveM2M(oppgaveId)
        assertNull(hentetOppgave.tilordnetRessurs)
        assertEquals(2, hentetOppgave.versjon)
    }

    @Test
    fun `ferdigstillOppgave setter status FERDIGSTILT, tømmer tilordnetRessurs og øker versjon`() {
        val opprettetOppgave = mockOppgaveClient.opprettOppgaveM2M(lagOppgaveRequest(tilordnetRessurs = "Z990001"))
        val oppgaveId = checkNotNull(opprettetOppgave.id)

        mockOppgaveClient.ferdigstillOppgave(oppgaveId, 1)

        val hentetOppgave = mockOppgaveClient.hentOppgaveM2M(oppgaveId)
        assertEquals(StatusEnum.FERDIGSTILT, hentetOppgave.status)
        assertNull(hentetOppgave.tilordnetRessurs)
        assertEquals(2, hentetOppgave.versjon)
    }

    @Test
    fun `hentOppgaveM2M for ukjent id gir fallback-oppgave som kan fordeles videre`() {
        val ukjentOppgaveId = 42L

        val fallbackOppgave = mockOppgaveClient.hentOppgaveM2M(ukjentOppgaveId)

        assertEquals("4415", fallbackOppgave.tildeltEnhetsnr)
        assertEquals(1, fallbackOppgave.versjon)

        mockOppgaveClient.fordelOppgave(ukjentOppgaveId, "Z990003", checkNotNull(fallbackOppgave.versjon))

        val fordeltOppgave = mockOppgaveClient.hentOppgaveM2M(ukjentOppgaveId)
        assertEquals("Z990003", fordeltOppgave.tilordnetRessurs)
        assertEquals(2, fordeltOppgave.versjon)
    }

    private fun lagOppgaveRequest(tilordnetRessurs: String? = null): LagOppgaveRequest =
        LagOppgaveRequest(
            personident = "12345678901",
            saksreferanse = "sak-1",
            prioritet = OppgavePrioritet.NORM,
            tema = Tema.GRU,
            behandlingstema = "ab0132",
            fristFerdigstillelse = "2026-12-01",
            aktivDato = "2026-11-24",
            oppgavetype = OppgavetypeGRU.BEH_SAK,
            beskrivelse = "Testoppgave",
            tilordnetRessurs = tilordnetRessurs,
            behandlesAvApplikasjon = "grunn-og-hjelpestonad",
            tildeltEnhetsnr = "4420",
        )
}
