package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingRepository
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.vedtak.AktivitetstypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.GrunnstønadPeriode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class TilkjentYtelseServiceTest : SpringContextTest() {
    @Autowired
    private lateinit var tilkjentYtelseService: TilkjentYtelseService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    fun `lagre tilkjent ytelse med andeler fra vedtak`() {
        val behandlingId = opprettBehandling()
        val vedtak =
            lagInnvilgetVedtak(
                behandlingId = behandlingId,
                perioder =
                    listOf(
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 3),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
            )

        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(behandlingId, vedtak)

        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)
        assertThat(tilkjentYtelse).isNotNull
        assertThat(tilkjentYtelse!!.behandlingId).isEqualTo(behandlingId)
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)

        val andel = tilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(andel.tom).isEqualTo(LocalDate.of(2025, 3, 31))
        assertThat(andel.beløp).isGreaterThan(0)
        assertThat(andel.kildeBehandlingId).isEqualTo(behandlingId)
    }

    @Test
    fun `skal oppdatere tilkjent ytelse med nye andeler ved nytt vedtak`() {
        val behandlingId = opprettBehandling()
        val vedtakFørste =
            lagInnvilgetVedtak(
                behandlingId = behandlingId,
                perioder =
                    listOf(
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 3),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
            )
        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(behandlingId, vedtakFørste)

        val vedtakOppdatert =
            lagInnvilgetVedtak(
                behandlingId = behandlingId,
                perioder =
                    listOf(
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 6),
                            utgifter = BigDecimal(8000),
                        ),
                    ),
            )
        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(behandlingId, vedtakOppdatert)

        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)

        val andel = tilkjentYtelse.andelerTilkjentYtelse.single()
        assertThat(andel.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(andel.tom).isEqualTo(LocalDate.of(2025, 6, 30))
    }

    @Test
    fun `opphør av alle andeler skal gi tom andelliste og beholde startmåned fra forrige behandling`() {
        val fagsakId = opprettFagsak()
        val førstegangsbehandlingId = opprettBehandling(fagsakId)
        val innvilget =
            lagInnvilgetVedtak(
                behandlingId = førstegangsbehandlingId,
                perioder =
                    listOf(
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 6),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
            )
        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(førstegangsbehandlingId, innvilget)

        val revurderingId = opprettBehandling(fagsakId, forrigeBehandlingId = førstegangsbehandlingId)
        val opphør =
            Vedtak(
                behandlingId = revurderingId,
                resultatType = ResultatType.OPPHØR,
                grunnstønadPerioder = emptyList(),
                saksbehandlerIdent = "VL",
                opphørFom = YearMonth.of(2025, 1),
            )
        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(revurderingId, opphør)

        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(revurderingId)!!
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).isEmpty()
        assertThat(tilkjentYtelse.startmåned).isEqualTo(YearMonth.of(2025, 1))
    }

    @Test
    fun `ingen_stønad-perioder skal ikke gi andeler`() {
        val behandlingId = opprettBehandling()
        val vedtak =
            lagInnvilgetVedtak(
                behandlingId = behandlingId,
                perioder =
                    listOf(
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 2),
                            utgifter = BigDecimal(5000),
                        ),
                        GrunnstønadPeriode(
                            datoFra = YearMonth.of(2025, 3),
                            datoTil = YearMonth.of(2025, 4),
                            utgifter = BigDecimal.ZERO,
                            barn = emptyList(),
                            periodetype = PeriodetypeBarnetilsyn.INGEN_STØNAD,
                            aktivitetstype = AktivitetstypeBarnetilsyn.IKKE_RELEVANT,
                        ),
                        lagBarnetilsynperiode(
                            datoFra = YearMonth.of(2025, 5),
                            datoTil = YearMonth.of(2025, 6),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
            )

        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(behandlingId, vedtak)

        val andeler = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)!!.andelerTilkjentYtelse
        assertThat(andeler).hasSize(2)
        assertThat(andeler.map { it.fom }).containsExactlyInAnyOrder(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 5, 1),
        )
    }

    private fun opprettFagsak(): UUID {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident("01010199998"))))
        return fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN)).id
    }

    private fun opprettBehandling(
        fagsakId: UUID = opprettFagsak(),
        forrigeBehandlingId: UUID? = null,
    ): UUID =
        behandlingRepository
            .insert(
                Behandling(
                    fagsakId = fagsakId,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.IKKE_SATT,
                    forrigeBehandlingId = forrigeBehandlingId,
                ),
            ).id

    private fun lagInnvilgetVedtak(
        behandlingId: UUID,
        perioder: List<GrunnstønadPeriode>,
    ) = Vedtak(
        behandlingId = behandlingId,
        resultatType = ResultatType.INNVILGET,
        grunnstønadPerioder = perioder,
        saksbehandlerIdent = "VL",
    )

    private fun lagBarnetilsynperiode(
        datoFra: YearMonth,
        datoTil: YearMonth,
        utgifter: BigDecimal,
    ) = GrunnstønadPeriode(
        datoFra = datoFra,
        datoTil = datoTil,
        utgifter = utgifter,
        barn = listOf(UUID.randomUUID()),
        periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
    )
}
