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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    internal fun `skal lagre og lese tilkjent ytelse med startmåned og andeler`() {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident("01010199997"))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
        val behandling = behandlingRepository.insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))

        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                startmåned = YearMonth.of(2025, 2),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(
                            beløp = 1000,
                            fom = LocalDate.of(2025, 2, 1),
                            tom = LocalDate.of(2025, 4, 30),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            ),
        )

        val lest = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!

        assertThat(lest.startmåned).isEqualTo(YearMonth.of(2025, 2))
        assertThat(lest.andelerTilkjentYtelse).hasSize(1)
        val andel = lest.andelerTilkjentYtelse.single()
        assertThat(andel.beløp).isEqualTo(1000)
        assertThat(andel.fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(andel.tom).isEqualTo(LocalDate.of(2025, 4, 30))
        assertThat(andel.kildeBehandlingId).isEqualTo(behandling.id)
    }
}
