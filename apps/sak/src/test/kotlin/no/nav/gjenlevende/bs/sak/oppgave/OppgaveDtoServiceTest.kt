package no.nav.gjenlevende.bs.sak.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

class OppgaveDtoServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakPersonRepository = mockk<FagsakPersonRepository>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()

    private val service =
        OppgaveService(
            fagsakRepository = fagsakRepository,
            fagsakPersonRepository = fagsakPersonRepository,
            oppgaveClient = oppgaveClient,
            oppgaveRepository = oppgaveRepository,
            behandlingRepository = behandlingRepository,
        )

    @Test
    fun `oppretter oppgave og lagrer OppgaveEntity med riktig gsakOppgaveId`() {
        val fagsakPersonId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val gsakOppgaveId = 98765L

        val fagsakPerson =
            FagsakPerson(
                id = fagsakPersonId,
                identer = setOf(Personident(ident = "12345678910")),
            )
        val fagsak =
            Fagsak(
                id = fagsakId,
                fagsakPersonId = fagsakPersonId,
                eksternId = 1001,
                stønadstype = StønadType.BARNETILSYN,
            )
        val behandling =
            Behandling(
                id = behandlingId,
                fagsakId = fagsakId,
                status = BehandlingStatus.OPPRETTET,
                resultat = BehandlingResultat.IKKE_SATT,
            )

        every { fagsakRepository.findByIdOrNull(fagsakId) } returns fagsak
        every { fagsakPersonRepository.findByIdOrNull(fagsakPersonId) } returns fagsakPerson
        every { oppgaveClient.opprettOppgaveM2M(oppgaveRequest = any()) } returns
            OppgaveDto(
                id = gsakOppgaveId,
                tema = Tema.EYO,
                oppgavetype = OppgavetypeEYO.BEH_SAK.name,
            )

        val entitySlot = slot<Oppgave>()
        every { oppgaveRepository.insert(capture(entitySlot)) } answers { firstArg() }

        service.opprettBehandleSakOppgave(
            behandling = behandling,
            saksbehandler = "A123456",
            tildeltEnhetsnr = "4415",
        )

        assertThat(entitySlot.captured.behandlingId).isEqualTo(behandlingId)
        assertThat(entitySlot.captured.gsakOppgaveId).isEqualTo(gsakOppgaveId)
        assertThat(entitySlot.captured.type).isEqualTo(OppgavetypeEYO.BEH_SAK.name)
    }

    @Test
    fun `får feil når oppgave respons mangler id`() {
        val fagsakPersonId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()

        val fagsakPerson =
            FagsakPerson(
                id = fagsakPersonId,
                identer = setOf(Personident(ident = "12345678910")),
            )
        val fagsak =
            Fagsak(
                id = fagsakId,
                fagsakPersonId = fagsakPersonId,
                eksternId = 1001,
                stønadstype = StønadType.BARNETILSYN,
            )
        val behandling =
            Behandling(
                id = behandlingId,
                fagsakId = fagsakId,
                status = BehandlingStatus.OPPRETTET,
                resultat = BehandlingResultat.IKKE_SATT,
            )

        every { fagsakRepository.findByIdOrNull(fagsakId) } returns fagsak
        every { fagsakPersonRepository.findByIdOrNull(fagsakPersonId) } returns fagsakPerson
        every { oppgaveClient.opprettOppgaveM2M(oppgaveRequest = any()) } returns
            OppgaveDto(
                id = null,
                tema = Tema.EYO,
                oppgavetype = OppgavetypeEYO.BEH_SAK.name,
            )

        assertThatThrownBy {
            service.opprettBehandleSakOppgave(
                behandling = behandling,
                saksbehandler = "A123456",
                tildeltEnhetsnr = "4415",
            )
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Oppgave-respons mangler id")

        verify(exactly = 0) { oppgaveRepository.insert(any()) }
    }
}
