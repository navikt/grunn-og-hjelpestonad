package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class IverksettOppdragTaskTest {
    private val oppdragIverksettingService = mockk<OppdragIverksettingService>()
    private val klokke = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC)
    private val task = IverksettOppdragTask(oppdragIverksettingService, klokke)

    @Test
    fun `sendt oppdrag oppretter status-task med første forsøk etter 30 sekunder`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns iverksettingsresultat(sendt = true)

        task.utfor(kontekst)

        verify(exactly = 1) {
            kontekst.opprettNesteTask(
                type = HentOppdragStatusTask.TYPE,
                payload = HentOppdragStatusTask.HentOppdragStatusTaskData(behandlingId, forsøk = 1),
                triggerTid = klokke.instant().plusSeconds(30),
            )
        }
    }

    @Test
    fun `ikke sendt oppdrag oppretter ingen task`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns iverksettingsresultat(sendt = false)

        task.utfor(kontekst)

        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `null resultat oppretter ingen task`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z123456"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns null

        task.utfor(kontekst)

        verifiserIngenNesteTask(kontekst)
    }

    @Test
    fun `payloadens saksbehandlerId sendes videre til iverksetting`() {
        val behandlingId = UUID.randomUUID()
        val saksbehandlerId = "Z654321"
        val kontekst = kontekst(behandlingId, saksbehandlerId)
        every { oppdragIverksettingService.iverksett(behandlingId, saksbehandlerId) } returns null

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

    private fun iverksettingsresultat(sendt: Boolean): Iverksettingsresultat {
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
        return Iverksettingsresultat(
            oppdrag =
                OppdragResultat(
                    behandlingId = behandlingId,
                    fagsakId = UUID.randomUUID(),
                    utbetalingsoppdrag = utbetalingsoppdrag,
                    status = OppdragStatus.OPPRETTET,
                    andeler = emptySet(),
                ),
            testandel = null,
            sendt = sendt,
        )
    }
}
