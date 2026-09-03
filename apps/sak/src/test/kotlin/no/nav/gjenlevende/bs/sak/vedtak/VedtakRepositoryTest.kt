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
import no.nav.gjenlevende.bs.sak.vedtak.AktivitetstypeBarnetilsyn
import no.nav.gjenlevende.bs.sak.vedtak.Barnetilsynperiode
import no.nav.gjenlevende.bs.sak.vedtak.PeriodetypeBarnetilsyn
import no.nav.gjenlevende.bs.sak.vedtak.ResultatType
import no.nav.gjenlevende.bs.sak.vedtak.Vedtak
import no.nav.gjenlevende.bs.sak.vedtak.VedtakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class VedtakRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    internal fun `skal lagre vedtak med riktige felter`() {
        val ident = "01010199998"
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(ident))))

        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))

        val behandling = behandlingRepository.insert(Behandling(UUID.randomUUID(), fagsak.id, null, BehandlingStatus.UTREDES, BehandlingResultat.IKKE_SATT))

        val vedtak =
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGET,
                barnetilsynperioder =
                    listOf(
                        Barnetilsynperiode(
                            datoFra = YearMonth.now(),
                            datoTil = YearMonth.now(),
                            utgifter = BigDecimal(1000),
                            barn = listOf(UUID.randomUUID()),
                            periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                            aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
                        ),
                    ),
                saksbehandlerIdent = "VL",
                opprettetAv = "VL",
                opprettetTid = LocalDateTime.now(),
            )

        vedtakRepository.insert(vedtak)

        assertThat(vedtakRepository.findByBehandlingId(behandling.id))
            .usingRecursiveComparison()
            .ignoringFields("id", "opprettetTid", "barnetilsynperioder.id")
            .isEqualTo(vedtak)
    }
}
