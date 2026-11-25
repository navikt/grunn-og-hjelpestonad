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
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.SimuleringRepository
import no.nav.grunn.og.hjelpestonad.oppdrag.simulering.Simuleringsstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class TilkjentYtelseServiceTest : SpringContextTest() {
    @Autowired
    private lateinit var tilkjentYtelseService: TilkjentYtelseService

    @Autowired
    private lateinit var simuleringRepository: SimuleringRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Test
    internal fun `slettTilkjentYtelseHvisFinnes sletter også simuleringen`() {
        val behandling = opprettBehandling()
        tilkjentYtelseService.lagre(
            TilkjentYtelse(
                behandlingId = behandling.id,
                startmåned = YearMonth.of(2025, 2),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(
                            beløp = BigDecimal(1000),
                            fom = LocalDate.of(2025, 2, 1),
                            tom = LocalDate.of(2025, 4, 30),
                            kildeBehandlingId = behandling.id,
                        ),
                    ),
            ),
        )
        simuleringRepository.lagre(
            behandling.id,
            Simuleringsstatus.SIMULERT,
            jacksonObjectMapper().readTree("""{"simuleringMottaker":[]}"""),
            LocalDateTime.parse("2026-11-19T11:15:30.123456"),
        )

        tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandling.id)

        assertThat(tilkjentYtelseService.hentTilkjentYtelse(behandling.id)).isNull()
        assertThat(simuleringRepository.finn(behandling.id)).isNull()
    }

    private fun opprettBehandling(): Behandling {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident("01010199997"))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.GRUNNSTØNAD))
        return behandlingRepository.insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
    }
}
