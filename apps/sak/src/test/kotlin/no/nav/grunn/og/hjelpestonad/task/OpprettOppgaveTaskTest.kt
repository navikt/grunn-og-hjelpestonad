package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class OpprettOppgaveTaskTest {
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val task = OpprettOppgaveTask(oppgaveService)

    @Test
    fun `oppgaveopprettelse forsøkes ikke på nytt automatisk`() {
        assertThat(task.retry.maxAntallFeil).isEqualTo(1)
    }

    @Test
    fun `behandle vedtak oppretter oppgave til beslutter`() {
        val idempotensUuid = UUID.randomUUID()
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.BEH_VED, idempotensUuid = idempotensUuid)

        task.utfor(kontekst(data))

        verify(exactly = 1) {
            oppgaveService.opprettBehandleVedtakOppgave(behandlingId = data.behandlingId, idempotensUuid = idempotensUuid)
        }
        verify(exactly = 0) { oppgaveService.opprettBehandleUnderkjentVedtakOppgave(any(), any(), any()) }
    }

    @Test
    fun `behandle sak etter underkjenning tildeles opprinnelig saksbehandler`() {
        val idempotensUuid = UUID.randomUUID()
        val data = OpprettOppgaveTaskData(UUID.randomUUID(), OppgavetypeGRU.BEH_SAK, "Z123456", idempotensUuid = idempotensUuid)

        task.utfor(kontekst(data))

        verify(exactly = 1) {
            oppgaveService.opprettBehandleUnderkjentVedtakOppgave(
                behandlingId = data.behandlingId,
                tilordnetSaksbehandler = "Z123456",
                idempotensUuid = idempotensUuid,
            )
        }
        verify(exactly = 0) { oppgaveService.opprettBehandleVedtakOppgave(any(), any()) }
    }

    @Test
    fun `omkjøring av lagret payload sender samme idempotensUuid til tjenesten`() {
        val behandlingId = UUID.randomUUID()
        val idempotensUuid = UUID.randomUUID()
        val lagretPayload =
            """{"behandlingId":"$behandlingId","oppgavetype":"BEH_VED","tilordnetSaksbehandler":null,"idempotensUuid":"$idempotensUuid"}"""

        task.utfor(kontekst(OpprettOppgaveTask.TYPE.deserialiser(lagretPayload)))
        task.utfor(kontekst(OpprettOppgaveTask.TYPE.deserialiser(lagretPayload)))

        verify(exactly = 2) {
            oppgaveService.opprettBehandleVedtakOppgave(behandlingId = behandlingId, idempotensUuid = idempotensUuid)
        }
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
