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
import no.nav.grunn.og.hjelpestonad.vedtak.Barnetilsynperiode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakRepository
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
