package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.SpringContextTest
import no.nav.gjenlevende.bs.sak.behandling.Behandling
import no.nav.gjenlevende.bs.sak.behandling.BehandlingRepository
import no.nav.gjenlevende.bs.sak.behandling.BehandlingResultat
import no.nav.gjenlevende.bs.sak.behandling.BehandlingStatus
import no.nav.gjenlevende.bs.sak.fagsak.FagsakPersonRepository
import no.nav.gjenlevende.bs.sak.fagsak.FagsakRepository
import no.nav.gjenlevende.bs.sak.fagsak.domain.Fagsak
import no.nav.gjenlevende.bs.sak.fagsak.domain.FagsakPerson
import no.nav.gjenlevende.bs.sak.fagsak.domain.Personident
import no.nav.gjenlevende.bs.sak.fagsak.domain.StønadType
import no.nav.gjenlevende.bs.sak.felles.sporbar.Endret
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class GjeldendeVedtakServiceTest : SpringContextTest() {
    @Autowired
    private lateinit var gjeldendeVedtakService: GjeldendeVedtakService

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    private lateinit var fagsak: Fagsak
    private val barnId1 = UUID.randomUUID()
    private val barnId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val fagsakPerson =
            fagsakPersonRepository.insert(
                FagsakPerson(identer = setOf(Personident("01010199999"))),
            )
        fagsak =
            fagsakRepository.insert(
                Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN),
            )
    }

    @Nested
    inner class HentVedtakFraDato {
        @Test
        fun `skal returnere tom liste når ingen vedtak finnes`() {
            val behandling = opprettFerdigstiltBehandling(BehandlingResultat.INNVILGET)

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).isEmpty()
        }

        @Test
        fun `skal returnere perioder fra ett vedtak`() {
            val behandling = opprettFerdigstiltBehandling(BehandlingResultat.INNVILGET)
            opprettVedtak(
                behandling,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))
        }

        @Test
        fun `skal filtrere bort perioder før fra-dato`() {
            val behandling = opprettFerdigstiltBehandling(BehandlingResultat.INNVILGET)
            opprettVedtak(
                behandling,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling.id, YearMonth.of(2025, 4))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 4))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
        }

        @Test
        fun `nyere vedtak skal overskrive eldre vedtak for overlappende måneder`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 3),
                        til = YearMonth.of(2025, 4),
                        utgifter = BigDecimal(2000),
                        barn = listOf(barnId1, barnId2),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling2.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(3)

            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 2))
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))
            assertThat(result.barnetilsynperioder[0].barn).containsExactly(barnId1)

            assertThat(result.barnetilsynperioder[1].datoFra).isEqualTo(YearMonth.of(2025, 3))
            assertThat(result.barnetilsynperioder[1].datoTil).isEqualTo(YearMonth.of(2025, 4))
            assertThat(result.barnetilsynperioder[1].utgifter).isEqualTo(BigDecimal(2000))
            assertThat(result.barnetilsynperioder[1].barn).containsExactlyInAnyOrder(barnId1, barnId2)

            assertThat(result.barnetilsynperioder[2].datoFra).isEqualTo(YearMonth.of(2025, 5))
            assertThat(result.barnetilsynperioder[2].datoTil).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[2].utgifter).isEqualTo(BigDecimal(1000))
        }

        @Test
        fun `opphør skal markere perioder fra opphørFom som INGEN_STØNAD`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 12),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.OPPHØRT,
                    endretTid = LocalDateTime.of(2025, 6, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.OPPHØR,
                emptyList(),
                opphørFom = YearMonth.of(2025, 7),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling2.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(2)

            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[0].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)

            assertThat(result.barnetilsynperioder[1].datoFra).isEqualTo(YearMonth.of(2025, 7))
            assertThat(result.barnetilsynperioder[1].datoTil).isEqualTo(YearMonth.of(2025, 12))
            assertThat(result.barnetilsynperioder[1].periodetype).isEqualTo(PeriodetypeBarnetilsyn.INGEN_STØNAD)
            assertThat(result.barnetilsynperioder[1].utgifter).isEqualTo(BigDecimal.ZERO)
            assertThat(result.barnetilsynperioder[1].barn).isEmpty()
        }

        @Test
        fun `nytt innvilget vedtak etter opphør skal legge til perioder igjen`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 12),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.OPPHØRT,
                    endretTid = LocalDateTime.of(2025, 6, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.OPPHØR,
                emptyList(),
                opphørFom = YearMonth.of(2025, 4),
            )

            val behandling3 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 9, 1, 10, 0),
                )
            opprettVedtak(
                behandling3,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 10),
                        til = YearMonth.of(2025, 12),
                        utgifter = BigDecimal(2000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling3.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(3)

            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 3))
            assertThat(result.barnetilsynperioder[0].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))

            assertThat(result.barnetilsynperioder[1].datoFra).isEqualTo(YearMonth.of(2025, 4))
            assertThat(result.barnetilsynperioder[1].datoTil).isEqualTo(YearMonth.of(2025, 9))
            assertThat(result.barnetilsynperioder[1].periodetype).isEqualTo(PeriodetypeBarnetilsyn.INGEN_STØNAD)
            assertThat(result.barnetilsynperioder[1].utgifter).isEqualTo(BigDecimal.ZERO)

            assertThat(result.barnetilsynperioder[2].datoFra).isEqualTo(YearMonth.of(2025, 10))
            assertThat(result.barnetilsynperioder[2].datoTil).isEqualTo(YearMonth.of(2025, 12))
            assertThat(result.barnetilsynperioder[2].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[2].utgifter).isEqualTo(BigDecimal(2000))
        }

        @Test
        fun `skal ignorere behandlinger som ikke er ferdigstilt`() {
            val ferdigstiltBehandling =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                ferdigstiltBehandling,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val ikkeFerdigstiltBehandling =
                opprettBehandling(
                    BehandlingResultat.INNVILGET,
                    BehandlingStatus.UTREDES,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                ikkeFerdigstiltBehandling,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(9999),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(ferdigstiltBehandling.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))
        }

        @Test
        fun `skal ignorere vedtak som ikke er INNVILGET eller OPPHØR`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.AVSLÅTT,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.AVSLÅTT,
                emptyList(),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling1.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))
        }

        @Test
        fun `skal slå sammen perioder med samme barn uavhengig av rekkefølge`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 3),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1, barnId2),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 4),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId2, barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling2.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[0].barn).containsExactlyInAnyOrder(barnId1, barnId2)
        }

        @Test
        fun `skal slå sammen sammenhengende perioder med like data`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 3),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 4),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling2.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(1)
            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
        }

        @Test
        fun `skal fylle gap mellom perioder med INGEN_STØNAD`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 3),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 6),
                        til = YearMonth.of(2025, 8),
                        utgifter = BigDecimal(2000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling2.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(3)

            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 3))
            assertThat(result.barnetilsynperioder[0].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))

            assertThat(result.barnetilsynperioder[1].datoFra).isEqualTo(YearMonth.of(2025, 4))
            assertThat(result.barnetilsynperioder[1].datoTil).isEqualTo(YearMonth.of(2025, 5))
            assertThat(result.barnetilsynperioder[1].periodetype).isEqualTo(PeriodetypeBarnetilsyn.INGEN_STØNAD)
            assertThat(result.barnetilsynperioder[1].utgifter).isEqualTo(BigDecimal.ZERO)
            assertThat(result.barnetilsynperioder[1].barn).isEmpty()

            assertThat(result.barnetilsynperioder[2].datoFra).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[2].datoTil).isEqualTo(YearMonth.of(2025, 8))
            assertThat(result.barnetilsynperioder[2].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[2].utgifter).isEqualTo(BigDecimal(2000))
        }

        @Test
        fun `Skal splitte opp vedtak som blir overskrevet av nyere vedtak`() {
            val behandling1 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 1, 1, 10, 0),
                )
            opprettVedtak(
                behandling1,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 1),
                        til = YearMonth.of(2025, 3),
                        utgifter = BigDecimal(1000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling2 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 2, 1, 10, 0),
                )
            opprettVedtak(
                behandling2,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 3),
                        til = YearMonth.of(2025, 6),
                        utgifter = BigDecimal(2000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val behandling3 =
                opprettFerdigstiltBehandling(
                    BehandlingResultat.INNVILGET,
                    endretTid = LocalDateTime.of(2025, 7, 1, 10, 0),
                )
            opprettVedtak(
                behandling3,
                ResultatType.INNVILGET,
                listOf(
                    lagBarnetilsynperiode(
                        fra = YearMonth.of(2025, 7),
                        til = YearMonth.of(2025, 8),
                        utgifter = BigDecimal(3000),
                        barn = listOf(barnId1),
                    ),
                ),
            )

            val result = gjeldendeVedtakService.hentGjeldendeVedtakFraDato(behandling3.id, YearMonth.of(2025, 1))

            assertThat(result.barnetilsynperioder).hasSize(3)

            assertThat(result.barnetilsynperioder[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
            assertThat(result.barnetilsynperioder[0].datoTil).isEqualTo(YearMonth.of(2025, 2))
            assertThat(result.barnetilsynperioder[0].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[0].utgifter).isEqualTo(BigDecimal(1000))

            assertThat(result.barnetilsynperioder[1].datoFra).isEqualTo(YearMonth.of(2025, 3))
            assertThat(result.barnetilsynperioder[1].datoTil).isEqualTo(YearMonth.of(2025, 6))
            assertThat(result.barnetilsynperioder[1].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[1].utgifter).isEqualTo(BigDecimal(2000))

            assertThat(result.barnetilsynperioder[2].datoFra).isEqualTo(YearMonth.of(2025, 7))
            assertThat(result.barnetilsynperioder[2].datoTil).isEqualTo(YearMonth.of(2025, 8))
            assertThat(result.barnetilsynperioder[2].periodetype).isEqualTo(PeriodetypeBarnetilsyn.ORDINÆR)
            assertThat(result.barnetilsynperioder[2].utgifter).isEqualTo(BigDecimal(3000))
        }
    }

    private fun opprettFerdigstiltBehandling(
        resultat: BehandlingResultat,
        endretTid: LocalDateTime = LocalDateTime.now(),
    ): Behandling = opprettBehandling(resultat, BehandlingStatus.FERDIGSTILT, endretTid)

    private fun opprettBehandling(
        resultat: BehandlingResultat,
        status: BehandlingStatus,
        endretTid: LocalDateTime = LocalDateTime.now(),
    ): Behandling {
        val behandling =
            Behandling(
                id = UUID.randomUUID(),
                fagsakId = fagsak.id,
                forrigeBehandlingId = null,
                status = status,
                resultat = resultat,
            )
        return behandlingRepository.insert(behandling).let { inserted ->
            val updated =
                inserted.copy(
                    sporbar = inserted.sporbar.copy(endret = Endret(endretTid = endretTid)),
                )
            behandlingRepository.update(updated)
            updated
        }
    }

    private fun opprettVedtak(
        behandling: Behandling,
        resultatType: ResultatType,
        perioder: List<Barnetilsynperiode>,
        opphørFom: YearMonth? = null,
    ) {
        vedtakRepository.insert(
            Vedtak(
                behandlingId = behandling.id,
                resultatType = resultatType,
                barnetilsynperioder = perioder,
                saksbehandlerIdent = "testbruker",
                opphørFom = opphørFom,
                opprettetAv = "testbruker",
                opprettetTid = LocalDateTime.now(),
            ),
        )
    }

    private fun lagBarnetilsynperiode(
        fra: YearMonth,
        til: YearMonth,
        utgifter: BigDecimal,
        barn: List<UUID>,
        periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetstype: AktivitetstypeBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
    ) = Barnetilsynperiode(
        datoFra = fra,
        datoTil = til,
        utgifter = utgifter,
        barn = barn,
        periodetype = periodetype,
        aktivitetstype = aktivitetstype,
    )
}
