package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.Transaksjon
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.infotrygd.InfotrygdFeedClient
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class InfotrygdFeedTaskTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val fagsakService = mockk<FagsakService>()
    private val infotrygdFeedClient = mockk<InfotrygdFeedClient>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    @Test
    fun `startbehandling bruker gjeldende personident`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandling = mockk<Behandling>()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandling.fagsakId } returns fagsakId
        every { fagsakService.hentAktivIdent(fagsakId) } returns "12345678901"

        val task = SendStartBehandlingTilInfotrygdFeedTask(behandlingService, fagsakService, infotrygdFeedClient)
        task.utfor(kontekst(behandlingId, SendStartBehandlingTilInfotrygdFeedTask.TYPE))

        verify { infotrygdFeedClient.sendStartBehandling("12345678901") }
    }

    @Test
    fun `vedtak uten barnetilsynsperioder sender første beregnede stønadsperiode`() {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandling = mockk<Behandling>()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandling.fagsakId } returns fagsakId
        every { fagsakService.hentAktivIdent(fagsakId) } returns "12345678901"
        every { vedtakService.hentVedtak(behandlingId) } returns
            Vedtak(
                behandlingId = behandlingId,
                resultatType = ResultatType.INNVILGET,
                saksbehandlerIdent = "Z123456",
                opprettetAv = "Z123456",
            )
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns
            TilkjentYtelse(
                behandlingId = behandlingId,
                startmåned = YearMonth.of(2026, 1),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(beløp = BigDecimal(752), fom = LocalDate.of(2026, 4, 1), tom = LocalDate.of(2026, 12, 31), kildeBehandlingId = behandlingId),
                        AndelTilkjentYtelse(beløp = BigDecimal(752), fom = LocalDate.of(2026, 2, 1), tom = LocalDate.of(2026, 3, 31), kildeBehandlingId = behandlingId),
                    ),
            )

        val task =
            SendVedtakTilInfotrygdFeedTask(
                behandlingService,
                vedtakService,
                fagsakService,
                infotrygdFeedClient,
                tilkjentYtelseService,
            )

        task.utfor(kontekst(behandlingId, SendVedtakTilInfotrygdFeedTask.TYPE))

        verify { infotrygdFeedClient.sendVedtak("12345678901", LocalDate.of(2026, 2, 1)) }
    }

    @Test
    fun `innvilget vedtak uten beregning sendes ikke til Infotrygd-feed`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns mockk()
        every { vedtakService.hentVedtak(behandlingId) } returns
            Vedtak(behandlingId = behandlingId, resultatType = ResultatType.INNVILGET, saksbehandlerIdent = "Z123456", opprettetAv = "Z123456")
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null

        val task = SendVedtakTilInfotrygdFeedTask(behandlingService, vedtakService, fagsakService, infotrygdFeedClient, tilkjentYtelseService)

        assertThrows<IllegalStateException> { task.utfor(kontekst(behandlingId, SendVedtakTilInfotrygdFeedTask.TYPE)) }
        verify(exactly = 0) { infotrygdFeedClient.sendVedtak(any(), any()) }
    }

    @Test
    fun `vedtak task lagrer behandlingId som ra UUID-streng`() {
        val behandlingId = UUID.randomUUID()

        val payload = SendVedtakTilInfotrygdFeedTask.TYPE.serialiser(behandlingId)

        assertEquals(behandlingId.toString(), payload)
        assertEquals(behandlingId, SendVedtakTilInfotrygdFeedTask.TYPE.deserialiser(payload))
    }

    private fun kontekst(
        payload: UUID,
        type: TaskType<UUID>,
    ) = TaskKontekst(
        task =
            Task(
                id = 1,
                type = type.navn,
                status = Status.KJØRER,
                payload = type.serialiser(payload),
                triggerTid = Instant.now(),
                opprettetTid = Instant.now(),
                plukketTid = null,
                antallFeil = 0,
                stoppaarsak = null,
                versjon = 0,
                sistEndretTid = Instant.now(),
            ),
        payload = payload,
        transaksjon = mockk<Transaksjon>(),
        produsent = mockk<TaskProdusent>(),
    )
}
