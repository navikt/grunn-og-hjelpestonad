package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class HentOppdragStatusTaskTest {
    private val oppdragIverksettingService = mockk<OppdragIverksettingService>()
    private val klokke = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC)
    private val task = HentOppdragStatusTask(oppdragIverksettingService, klokke)

    @Test
    fun `KVITTERT_OK oppretter ingen ny task`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 1)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.KVITTERT_OK, null)

        task.utfor(kontekst)

        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    @Test
    fun `LAGT_PÅ_KØ på dagtid oppretter task med neste forsøk om 15 minutter`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 4)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.LAGT_PÅ_KØ, null)

        task.utfor(kontekst)

        verify(exactly = 1) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 5),
                triggerTid = klokke.instant().plusSeconds(15 * 60),
            )
        }
    }

    @Test
    fun `LAGT_PÅ_KØ fredag kveld venter til mandag kl 06`() {
        val fredagKveld = Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC)
        val behandlingId = UUID.randomUUID()
        val kontekst = kontekst(HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 1))
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.LAGT_PÅ_KØ, null)

        HentOppdragStatusTask(oppdragIverksettingService, fredagKveld).utfor(kontekst)

        verify(exactly = 1) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 2),
                triggerTid = Instant.parse("2026-11-16T05:00:00Z"),
            )
        }
    }

    @Test
    fun `LAGT_PÅ_KØ ved maks antall forsøk feiler uten å opprette task`() {
        val behandlingId = UUID.randomUUID()
        val payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, HentOppdragStatusTask.MAKS_FORSØK)
        val kontekst = kontekst(payload)
        every { oppdragIverksettingService.hentStatus(behandlingId) } returns OppdragStatusSvar(OppdragStatus.LAGT_PÅ_KØ, null)

        assertThatThrownBy { task.utfor(kontekst) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Fikk ikke kvittering fra OS for behandling=$behandlingId etter ${HentOppdragStatusTask.MAKS_FORSØK} forsøk")

        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    @Test
    fun `funksjonell feil feiler uten å ta med kvitteringsmeldingen`() {
        val behandlingId = UUID.randomUUID()
        val kontekst = kontekst(HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 1))
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
