package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class OpprettOppgaveTaskTest {
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val task = OpprettOppgaveTask(oppgaveService)

    @Test
    fun `godkjenn vedtak oppretter oppgave til beslutter`() {
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.GOD_VED)

        task.utfor(kontekst(data))

        verify(exactly = 1) { oppgaveService.opprettGodkjennVedtakOppgave(data.behandlingId) }
        verify(exactly = 0) { oppgaveService.opprettBehandleUnderkjentVedtakOppgave(any(), any()) }
    }

    @Test
    fun `behandle sak etter underkjenning tildeles opprinnelig saksbehandler`() {
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.BEH_SAK, "Z123456")

        task.utfor(kontekst(data))

        verify(exactly = 1) { oppgaveService.opprettBehandleUnderkjentVedtakOppgave(data.behandlingId, "Z123456") }
        verify(exactly = 0) { oppgaveService.opprettGodkjennVedtakOppgave(any()) }
    }

    @Test
    fun `behandle sak uten saksbehandler avvises`() {
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.BEH_SAK)

        assertThatThrownBy { task.utfor(kontekst(data)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("tilordnetSaksbehandler påkrevd for BEH_SAK")

        verify { oppgaveService wasNot Called }
    }

    @Test
    fun `andre oppgavetyper opprettes ikke automatisk`() {
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.BEH_SED)

        assertThatThrownBy { task.utfor(kontekst(data)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Ugyldig oppgavetype: BEH_SED")

        verify { oppgaveService wasNot Called }
    }

    private fun kontekst(data: OpprettOppgaveTaskData) =
        mockk<TaskKontekst<OpprettOppgaveTaskData>> {
            every { payload } returns data
        }
}
