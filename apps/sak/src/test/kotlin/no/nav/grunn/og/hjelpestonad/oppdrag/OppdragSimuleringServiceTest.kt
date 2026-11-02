package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional

class OppdragSimuleringServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val oppdragClient = mockk<OppdragClient>()

    private val fagsak = Fagsak(fagsakPersonId = java.util.UUID.randomUUID(), eksternId = 2L, stønadstype = StønadType.BARNETILSYN)
    private val behandling =
        Behandling(
            eksternId = 1L,
            fagsakId = fagsak.id,
            status = BehandlingStatus.UTREDES,
            resultat = BehandlingResultat.IKKE_SATT,
        )
    private val sendtOppdrag = slot<Utbetalingsoppdrag>()

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"
        every { behandlingService.hentBehandlingEllerKast(behandling.id) } returns behandling
        every { fagsakRepository.findById(fagsak.id) } returns Optional.of(fagsak)
        every { fagsakService.hentAktivIdent(fagsak.id) } returns "12345678910"
        every { oppdragClient.simuler(capture(sendtOppdrag)) } returns DetaljertSimuleringResultat()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    private fun service(brukTestandelHvisMangler: Boolean) =
        OppdragSimuleringService(
            behandlingService = behandlingService,
            fagsakRepository = fagsakRepository,
            fagsakService = fagsakService,
            tilkjentYtelseService = tilkjentYtelseService,
            oppdragClient = oppdragClient,
            meterRegistry = SimpleMeterRegistry(),
            brukTestandelHvisMangler = brukTestandelHvisMangler,
        )

    @Test
    fun `simulerer med andelene fra tilkjent ytelse når de finnes`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns
            TilkjentYtelse(
                behandlingId = behandling.id,
                startmåned = YearMonth.of(2026, 1),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(
                            beløp = 2500,
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 3, 31),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            )

        val respons = service(brukTestandelHvisMangler = true).simuler(behandling.id)

        assertThat(respons.testandel).isNull()
        assertThat(
            sendtOppdrag.captured.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("2500")
    }

    @Test
    fun `simulerer med testandel når tilkjent ytelse mangler og testandel er slått på`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null

        val respons = service(brukTestandelHvisMangler = true).simuler(behandling.id)

        val måned = YearMonth.now()
        assertThat(respons.testandel).isEqualTo(Testandel(fom = måned.atDay(1), tom = måned.atEndOfMonth(), beløp = 1000))
        assertThat(
            sendtOppdrag.captured.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("1000")
    }

    @Test
    fun `kaster bad request når tilkjent ytelse mangler og testandel er slått av`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null

        assertThatThrownBy { service(brukTestandelHvisMangler = false).simuler(behandling.id) }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST) }
    }
}
