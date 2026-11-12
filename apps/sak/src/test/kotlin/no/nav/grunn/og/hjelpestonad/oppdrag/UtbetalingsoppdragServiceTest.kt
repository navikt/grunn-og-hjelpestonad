package no.nav.grunn.og.hjelpestonad.oppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
    private val oppdragResultatRepository = mockk<OppdragResultatRepository>()

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
        every { behandlingService.hentBehandlingEllerKast(behandling.id) } returns behandling
        every { fagsakRepository.findById(fagsak.id) } returns Optional.of(fagsak)
        every { fagsakService.hentAktivIdent(fagsak.id) } returns "12345678910"
        every { oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, behandling.id) } returns emptyList()
        every { oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, behandling.id) } returns emptyList()
    }

    private fun service(brukTestandelHvisMangler: Boolean) =
        UtbetalingsoppdragService(
            behandlingService = behandlingService,
            fagsakRepository = fagsakRepository,
            fagsakService = fagsakService,
            tilkjentYtelseService = tilkjentYtelseService,
            oppdragResultatRepository = oppdragResultatRepository,
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
                            beløp = 2500.toBigDecimal(),
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 3, 31),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            )

        val resultat = service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id, "Z123456")

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

        val resultat = service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id, "Z123456")

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

        assertThatThrownBy { service(brukTestandelHvisMangler = false).lagUtbetalingsoppdrag(behandling.id, "Z123456") }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST) }
    }

    @Test
    fun `OppdragId bruker fagsystem, aktiv ident og ekstern behandlings-ID`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns null
        val service = service(brukTestandelHvisMangler = true)

        val oppdragId = service.lagOppdragId(behandling.id)
        val oppdrag = service.lagUtbetalingsoppdrag(behandling.id, "Z123456").utbetalingsoppdrag

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

    @Test
    fun `tilkjent ytelse uten andeler opphører kjeden når fagsaken har en`() {
        val sisteAndel =
            Utbetalingsandel(
                fom = LocalDate.of(2026, 1, 1),
                tom = LocalDate.of(2026, 6, 30),
                beløp = 1000,
                ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
                periodeId = 0,
                forrigePeriodeId = null,
                kildeBehandlingId = forrigeBehandlingId,
            )
        every { oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, behandling.id) } returns listOf(sisteAndel)
        every { oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, behandling.id) } returns listOf(sisteAndel)
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns tilkjentYtelseUtenAndeler()

        val resultat = service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id, "Z123456")

        assertThat(resultat.testandel).isNull()
        assertThat(
            resultat.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .opphør
                ?.opphørDatoFom,
        ).isEqualTo(LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `tilkjent ytelse uten andeler og uten kjede gir bad request i simulering når testandel er slått av`() {
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns tilkjentYtelseUtenAndeler()

        assertThatThrownBy { service(brukTestandelHvisMangler = false).lagUtbetalingsoppdrag(behandling.id, "Z123456") }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST) }
    }

    @Test
    fun `simulering uten endring mot forrige behandling gir bad request`() {
        val forrige =
            Utbetalingsandel(
                fom = LocalDate.of(2026, 1, 1),
                tom = LocalDate.of(2026, 3, 31),
                beløp = 2500,
                ytelsestype = GrunnOgHjelpYtelsestype.GRUNNSTØNAD,
                periodeId = 0,
                forrigePeriodeId = null,
                kildeBehandlingId = forrigeBehandlingId,
            )
        every { oppdragResultatRepository.finnAndelerFraForrigeBehandling(fagsak.id, behandling.id) } returns listOf(forrige)
        every { oppdragResultatRepository.finnSisteAndelPerKjede(fagsak.id, behandling.id) } returns listOf(forrige)
        every { tilkjentYtelseService.hentTilkjentYtelse(behandling.id) } returns
            TilkjentYtelse(
                behandlingId = behandling.id,
                startmåned = YearMonth.of(2026, 1),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(
                            beløp = 2500.toBigDecimal(),
                            fom = LocalDate.of(2026, 1, 1),
                            tom = LocalDate.of(2026, 3, 31),
                            kildeBehandlingId = forrigeBehandlingId,
                        ),
                    ),
            )

        assertThatThrownBy { service(brukTestandelHvisMangler = true).lagUtbetalingsoppdrag(behandling.id, "Z123456") }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST) }
    }

    private val forrigeBehandlingId = java.util.UUID.randomUUID()

    private fun tilkjentYtelseUtenAndeler() = TilkjentYtelse(behandlingId = behandling.id, startmåned = YearMonth.of(2026, 1), andelerTilkjentYtelse = emptySet())
}
