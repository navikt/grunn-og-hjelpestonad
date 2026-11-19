package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class HentOppdragStatusTaskTest {
    private val oppdragIverksettingService = mockk<OppdragIverksettingService>()
    private val task = HentOppdragStatusTask(oppdragIverksettingService)

    @Test
    fun `KVITTERT_OK oppretter ingen ny task`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.KVITTERT_OK, null)

        task.utfor(kontekst)

        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    @Test
    fun `LAGT_PÅ_KØ prøver samme task igjen om 10 sekunder`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.LAGT_PÅ_KØ, null)

        assertThatThrownBy { task.utfor(kontekst) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Venter på kvittering fra OS for behandling=$behandlingId")

        assertThat(task.retry.maxAntallFeil).isEqualTo(100)
        assertThat(task.retry.backoff(1)).isEqualTo(10.seconds)
        assertThat(task.retry.backoff(99)).isEqualTo(10.seconds)
        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    @Test
    fun `funksjonell feil feiler uten å ta med kvitteringsmeldingen`() {
        val behandlingId = UUID.randomUUID()
        val kontekst = kontekst(HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId))
        val kvitteringsmelding = "Kvitteringsdetalj med sensitiv informasjon"
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns
            OppdragStatusSvar("KVITTERT_FUNKSJONELL_FEIL", kvitteringsmelding)

        assertThatThrownBy { task.utfor(kontekst) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("KVITTERT_FUNKSJONELL_FEIL")
            .hasMessageNotContaining(kvitteringsmelding)
    }

    private fun kontekst(payload: HentOppdragStatusTask.HentOppdragStatusTaskData) =
        mockk<TaskKontekst<HentOppdragStatusTask.HentOppdragStatusTaskData>>(relaxed = true) {
            every { this@mockk.payload } returns payload
        }
}
