package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

/** Kjører mot Postgres, så migreringen V59 (gjeldende_steg) testes på ekte. */
class BehandlingRepositoryTest(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
) : SpringContextTest() {
    @Test
    fun `ny behandling starter på steget ÅRSAK`() {
        val behandlingId = opprettBehandling()

        assertThat(behandlingRepository.findById(behandlingId).orElseThrow().gjeldendeSteg).isEqualTo(BehandlingSteg.ÅRSAK)
    }

    @Test
    fun `settGjeldendeSteg lagrer det nye steget`() {
        val behandlingId = opprettBehandling()

        behandlingService.settGjeldendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)

        assertThat(behandlingRepository.findById(behandlingId).orElseThrow().gjeldendeSteg).isEqualTo(BehandlingSteg.INNGANGSVILKÅR)
    }

    private fun opprettBehandling(): UUID {
        val fagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(tilfeldigIdent()))))
        val fagsak = fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.GRUNNSTØNAD))
        return behandlingRepository
            .insert(Behandling(fagsakId = fagsak.id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT))
            .id
    }

    private fun tilfeldigIdent(): String = (10_000_000_000L..99_999_999_999L).random().toString()
}
