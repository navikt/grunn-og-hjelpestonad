package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

/** Kjører mot Postgres, så migreringen V59 (gjeldende_steg) testes på ekte. */
class BehandlingRepositoryTest(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingStegService: BehandlingStegService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val transactionTemplate: TransactionTemplate,
) : SpringContextTest() {
    @Test
    fun `ny behandling starter på steget ÅRSAK`() {
        val behandlingId = opprettBehandling()

        assertThat(behandlingRepository.findById(behandlingId).orElseThrow().gjeldendeSteg).isEqualTo(BehandlingSteg.ÅRSAK)
    }

    @Test
    fun `registrerLagretÅrsak lagrer steget INNGANGSVILKÅR`() {
        val behandlingId = opprettBehandling()

        behandlingStegService.registrerLagretÅrsak(behandlingId)

        assertThat(behandlingRepository.findById(behandlingId).orElseThrow().gjeldendeSteg).isEqualTo(BehandlingSteg.INNGANGSVILKÅR)
    }

    @Test
    fun `steget står uendret når transaksjonen rulles tilbake etter registrerLagretÅrsak`() {
        val behandlingId = opprettBehandling()

        assertThatThrownBy {
            transactionTemplate.executeWithoutResult {
                behandlingStegService.registrerLagretÅrsak(behandlingId)
                throw IllegalStateException("Simulert feil etter registrering av årsak")
            }
        }.hasMessage("Simulert feil etter registrering av årsak")

        // Hentes på nytt utenfor transaksjonen, så vi ser det som er lagret i databasen.
        assertThat(behandlingRepository.findById(behandlingId).orElseThrow().gjeldendeSteg).isEqualTo(BehandlingSteg.ÅRSAK)
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
