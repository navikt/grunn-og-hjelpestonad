package no.nav.grunn.og.hjelpestonad.oppdrag

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
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
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Optional

class UtbetalingsoppdragServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val fagsak = Fagsak(fagsakPersonId = java.util.UUID.randomUUID(), eksternId = 2L, stønadstype = StønadType.BARNETILSYN)
    private val behandling =
        Behandling(
            eksternId = 1L,
            fagsakId = fagsak.id,
            status = BehandlingStatus.UTREDES,
            resultat = BehandlingResultat.IKKE_SATT,
        )
    private val clock = Clock.fixed(LocalDate.of(2026, 11, 15).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant(), ZoneId.of("Europe/Oslo"))

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"
        every { behandlingService.hentBehandlingEllerKast(behandling.id) } returns behandling
        every { fagsakRepository.findById(fagsak.id) } returns Optional.of(fagsak)
        every { fagsakService.hentAktivIdent(fagsak.id) } returns "12345678910"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    private fun service(brukTestandelHvisMangler: Boolean) =
        UtbetalingsoppdragService(
            behandlingService = behandlingService,
            fagsakRepository = fagsakRepository,
            fagsakService = fagsakService,
            tilkjentYtelseService = tilkjentYtelseService,
            brukTestandelHvisMangler = brukTestandelHvisMangler,
            clock = clock,
        )

    @Test
    fun `lager oppdrag med andelene fra tilkjent ytelse når de finnes`() {
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

        val resultat = service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id)

        assertThat(resultat.testandel).isNull()
        assertThat(
            resultat.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("2500")
    }

    @Test
    fun `lager oppdrag med testandel når tilkjent ytelse mangler og testandel er slått på`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null

        val resultat = service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id)

        assertThat(resultat.testandel).isEqualTo(Testandel(fom = LocalDate.of(2026, 11, 1), tom = LocalDate.of(2026, 11, 30), beløp = 1000))
        assertThat(
            resultat.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("1000")
    }

    @Test
    fun `kaster bad request når tilkjent ytelse mangler og testandel er slått av`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null

        assertThatThrownBy { service(brukTestandelHvisMangler = false).lagUtbetalingsoppdrag(behandling.id) }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST) }
    }

    @Test
    fun `OppdragId bruker fagsystem, aktiv ident og ekstern behandlings-ID`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null
        val service = service(brukTestandelHvisMangler = true)

        val oppdragId = service.lagOppdragId(behandling.id)
        val oppdrag = service.lagUtbetalingsoppdrag(behandling.id).utbetalingsoppdrag

        assertThat(oppdragId).isEqualTo(OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "1"))
        assertThat(oppdragId.fagsystem).isEqualTo(oppdrag.fagSystem)
        assertThat(oppdragId.personIdent).isEqualTo(oppdrag.aktoer)
        assertThat(oppdragId.behandlingsId).isEqualTo(
            oppdrag.utbetalingsperiode
                .first()
                .behandlingId
                .toString(),
        )
    }
}
