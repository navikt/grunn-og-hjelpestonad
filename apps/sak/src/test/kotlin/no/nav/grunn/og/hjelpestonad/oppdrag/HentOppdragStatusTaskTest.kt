package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
    fun `LAGT_PÅ_KØ lar motoren prøve samme task igjen`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.LAGT_PÅ_KØ, null)

        assertThatThrownBy { task.utfor(kontekst) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Venter på kvittering fra OS for behandling=$behandlingId")

        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    @Test
    fun `retry starter raskt og dobler ventetiden opp til 15 minutter`() {
        val ventetider = (1 until task.retry.maxAntallFeil).map(task.retry.backoff)

        assertThat(ventetider.take(8)).containsExactly(
            10.seconds,
            20.seconds,
            40.seconds,
            80.seconds,
            160.seconds,
            320.seconds,
            640.seconds,
            15.minutes,
        )
        assertThat(ventetider.drop(7)).allMatch { it == 15.minutes }
    }

    @Test
    fun `retry fortsetter i omtrent ett døgn uten at planlagt ventetid overstiger 24 timer`() {
        val samletVentetid =
            (1 until task.retry.maxAntallFeil)
                .map(task.retry.backoff)
                .fold(Duration.ZERO) { samletVentetid, ventetid -> samletVentetid + ventetid }

        assertThat(samletVentetid).isEqualTo(23.hours + 51.minutes + 10.seconds)
        assertThat(samletVentetid <= 24.hours).isTrue()
        assertThat(samletVentetid + 15.minutes > 24.hours).isTrue()
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
