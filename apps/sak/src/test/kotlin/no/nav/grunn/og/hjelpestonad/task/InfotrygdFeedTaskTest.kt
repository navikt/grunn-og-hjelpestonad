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
import no.nav.grunn.og.hjelpestonad.vedtak.AktivitetstypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.GrunnstønadPeriode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.junit.jupiter.api.Test
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

    @Test
    fun `startbehandling bruker gjeldende personident`() {
        // Arrange
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandling = mockk<Behandling>()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandling.fagsakId } returns fagsakId
        every { fagsakService.hentAktivIdent(fagsakId) } returns "12345678901"

        // Act
        val task = SendStartBehandlingTilInfotrygdFeedTask(behandlingService, fagsakService, infotrygdFeedClient)
        task.utfor(kontekst(behandlingId.toString(), SendStartBehandlingTilInfotrygdFeedTask.TYPE))

        // Assert
        verify { infotrygdFeedClient.sendStartBehandling("12345678901") }
    }

    @Test
    fun `vedtak sender første ordinære stønadsperiode`() {
        // Arrange
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
                grunnstønadPerioder =
                    listOf(
                        periode(YearMonth.of(2026, 1), PeriodetypeBarnetilsyn.INGEN_STØNAD),
                        periode(YearMonth.of(2026, 2), PeriodetypeBarnetilsyn.ORDINÆR),
                    ),
                saksbehandlerIdent = "Z123456",
                opprettetAv = "Z123456",
            )

        val task =
            SendVedtakTilInfotrygdFeedTask(
                behandlingService,
                vedtakService,
                fagsakService,
                infotrygdFeedClient,
            )
        // Act
        task.utfor(kontekst(behandlingId.toString(), SendVedtakTilInfotrygdFeedTask.TYPE))

        // Assert
        verify { infotrygdFeedClient.sendVedtak("12345678901", LocalDate.of(2026, 2, 1)) }
    }

    private fun periode(
        datoFra: YearMonth,
        periodetype: PeriodetypeBarnetilsyn,
    ) = GrunnstønadPeriode(
        datoFra = datoFra,
        datoTil = datoFra,
        utgifter = BigDecimal.ZERO,
        barn = emptyList(),
        periodetype = periodetype,
        aktivitetstype = AktivitetstypeBarnetilsyn.IKKE_RELEVANT,
    )

    private fun kontekst(
        payload: String,
        type: TaskType<String>,
    ) = TaskKontekst(
        task =
            Task(
                id = 1,
                type = type.navn,
                status = Status.KJØRER,
                payload = payload,
                triggerTid = Instant.now(),
                opprettetTid = Instant.now(),
                plukketTid = null,
                antallFeil = 0,
                stoppaarsak = null,
                versjon = 0,
            ),
        payload = payload,
        transaksjon = mockk<Transaksjon>(),
        produsent = mockk<TaskProdusent>(),
    )
}
