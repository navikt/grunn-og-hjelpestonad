package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class IverksettOppdragTaskTest {
    private val oppdragIverksettingService = mockk<OppdragIverksettingService>()
    private val klokke = Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC)
    private val task = IverksettOppdragTask(oppdragIverksettingService, klokke)

    @Test
    fun `feil ved sending prøves igjen etter 15 minutter uten å opprette statustask`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        val sendingFeilet = IllegalStateException("Sending feilet")
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } throws sendingFeilet

        assertThatThrownBy { task.utfor(kontekst) }.isSameAs(sendingFeilet)

        assertThat(task.retry.maxAntallFeil).isEqualTo(3)
        assertThat(task.retry.backoff(1)).isEqualTo(15.minutes)
        assertThat(task.retry.backoff(2)).isEqualTo(15.minutes)
        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `sendt oppdrag oppretter status-task etter 30 sekunder også utenfor åpningstid`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns
            Iverksettingsresultat.Sendt(oppdragResultat = oppdragResultat())

        task.utfor(kontekst)

        verify(exactly = 1) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId),
                triggerTid = klokke.instant().plusSeconds(30),
            )
        }
    }

    @Test
    fun `allerede iverksatt oppdrag på kø oppretter status-task, fordi testendepunktet ikke oppretter en`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns
            Iverksettingsresultat.AlleredeIverksatt(oppdragResultat = oppdragResultat(OppdragStatus.LAGT_PÅ_KØ))

        task.utfor(kontekst)

        verify(exactly = 1) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId),
                triggerTid = klokke.instant().plusSeconds(30),
            )
        }
    }

    @Test
    fun `allerede kvittert oppdrag oppretter ingen task`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns
            Iverksettingsresultat.AlleredeIverksatt(oppdragResultat = oppdragResultat(OppdragStatus.KVITTERT_OK))

        task.utfor(kontekst)

        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `oppdrag uten endring oppretter ingen task`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns
            Iverksettingsresultat.IngenEndring(oppdragResultat = oppdragResultat())

        task.utfor(kontekst)

        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `ingenting å sende oppretter ingen task`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns Iverksettingsresultat.IngentingÅSende

        task.utfor(kontekst)

        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `payloadens saksbehandlerId sendes videre til iverksetting`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z654321"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns Iverksettingsresultat.IngentingÅSende

        task.utfor(kontekst)

        verify(exactly = 1) { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) }
    }

    private fun kontekst(
        behandlingId: UUID,
        saksbehandlerId: String,
    ) = mockk<TaskKontekst<IverksettOppdragTask.IverksettOppdragTaskData>>(relaxed = true) {
        every { this@mockk.payload } returns IverksettOppdragTask.IverksettOppdragTaskData(behandlingId, saksbehandlerId)
    }

    private fun verifiserIngenNesteTask(kontekst: TaskKontekst<IverksettOppdragTask.IverksettOppdragTaskData>) {
        verify(exactly = 0) {
            kontekst.opprettNesteTask<HentOppdragStatusTask.HentOppdragStatusTaskData>(any(), any(), any())
        }
    }

    private fun oppdragResultat(status: String = OppdragStatus.OPPRETTET): OppdragResultat {
        val behandlingId = UUID.randomUUID()
        val utbetalingsoppdrag =
            lagUtbetalingsoppdrag(
                Oppdragsgrunnlag(
                    behandlingId = behandlingId,
                    eksternBehandlingId = 1L,
                    eksternFagsakId = 2L,
                    personIdent = "12345678910",
                    saksbehandlerId = "Z123456",
                    vedtaksdato = LocalDate.of(2026, 11, 1),
                    andelerTilkjentYtelse =
                        listOf(
                            AndelTilkjentYtelse(
                                beløp = 1000.toBigDecimal(),
                                fom = LocalDate.of(2026, 11, 1),
                                tom = LocalDate.of(2026, 11, 30),
                                kildeBehandlingId = behandlingId,
                            ),
                        ),
                ),
            ).utbetalingsoppdrag
        return OppdragResultat(
            behandlingId = behandlingId,
            fagsakId = UUID.randomUUID(),
            utbetalingsoppdrag = utbetalingsoppdrag,
            status = status,
            andeler = emptySet(),
        )
    }
}
