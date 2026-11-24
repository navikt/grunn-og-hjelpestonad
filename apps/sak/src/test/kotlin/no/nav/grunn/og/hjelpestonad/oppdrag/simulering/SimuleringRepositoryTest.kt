package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

internal class SimuleringRepositoryTest : SpringContextTest() {
    @Autowired
    private lateinit var simuleringRepository: SimuleringRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    /** Kjøres før `reset()` i [SpringContextTest]: `simulering` har FK til `behandling`, og den tabellen slettes ikke der. */
    @AfterEach
    fun slettSimuleringer() {
        jdbcTemplate.update("DELETE FROM simulering")
    }

    @Test
    fun `lagre og finn gir samme status, respons og tidspunkt`() {
        val behandling = opprettBehandling()
        val respons = lagRespons()
        // Mikrosekunder med vilje: TIMESTAMP skal ikke runde av til millisekunder.
        val tidspunkt = LocalDateTime.parse("2026-11-19T11:15:30.123456")

        simuleringRepository.lagre(behandling.id, Simuleringsstatus.SIMULERT, respons, tidspunkt)

        val lagret = simuleringRepository.finn(behandling.id)!!
        assertThat(lagret.behandlingId).isEqualTo(behandling.id)
        assertThat(lagret.status).isEqualTo(Simuleringsstatus.SIMULERT)
        assertThat(lagret.respons).isEqualTo(respons)
        assertThat(lagret.opprettetTid).isEqualTo(tidspunkt)
    }

    @Test
    fun `ny lagre overskriver forrige simulering for behandlingen`() {
        val behandling = opprettBehandling()
        simuleringRepository.lagre(behandling.id, Simuleringsstatus.SIMULERT, lagRespons(), LocalDateTime.parse("2026-11-19T11:15:30.123456"))
        val nyttTidspunkt = LocalDateTime.parse("2026-11-20T09:00:00.654321")

        simuleringRepository.lagre(behandling.id, Simuleringsstatus.FEILET, null, nyttTidspunkt)

        val lagret = simuleringRepository.finn(behandling.id)!!
        assertThat(lagret.status).isEqualTo(Simuleringsstatus.FEILET)
        assertThat(lagret.respons).isNull()
        assertThat(lagret.opprettetTid).isEqualTo(nyttTidspunkt)
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM simulering WHERE behandling_id = ?", Int::class.java, behandling.id))
            .isEqualTo(1)
    }

    @Test
    fun `respons null lagres som null`() {
        val behandling = opprettBehandling()

        simuleringRepository.lagre(behandling.id, Simuleringsstatus.INGEN_ENDRING, null, LocalDateTime.parse("2026-11-19T11:15:30.123456"))

        val lagret = simuleringRepository.finn(behandling.id)!!
        assertThat(lagret.status).isEqualTo(Simuleringsstatus.INGEN_ENDRING)
        assertThat(lagret.respons).isNull()
    }

    @Test
    fun `slett fjerner raden`() {
        val behandling = opprettBehandling()
        simuleringRepository.lagre(behandling.id, Simuleringsstatus.SIMULERT, lagRespons(), LocalDateTime.parse("2026-11-19T11:15:30.123456"))

        simuleringRepository.slett(behandling.id)

        assertThat(simuleringRepository.finn(behandling.id)).isNull()
    }

    private fun lagRespons() = jacksonObjectMapper().readTree("""{"simuleringMottaker":[{"mottakerNummer":"01010199997","simulertPostering":[{"beløp":752}]}]}""")

    private fun opprettBehandling(): Behandling {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(PERSONIDENT))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.GRUNNSTØNAD))
        return behandlingRepository.insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
    }

    companion object {
        // Syntetisk fødselsnummer.
        private const val PERSONIDENT = "01010199997"
    }
}
