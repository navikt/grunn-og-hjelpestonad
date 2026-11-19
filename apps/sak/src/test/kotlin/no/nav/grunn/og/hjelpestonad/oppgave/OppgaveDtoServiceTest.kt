package no.nav.grunn.og.hjelpestonad.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingRepository
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
            behandlingService = BehandlingService(behandlingRepository),
        )

    @ParameterizedTest
    @CsvSource(
        "GRUNNSTØNAD, ab0132, false",
        "GRUNNSTØNAD, ab0132, true",
        "HJELPESTØNAD, ab0138, false",
        "HJELPESTØNAD, ab0138, true",
    )
    fun `alle oppgaver bruker riktig tema og null behandlingstype gjennom gjentatte beslutningsrunder`(
        stønadstype: StønadType,
        behandlingstema: String,
        revurdering: Boolean,
    ) {
        val person = FagsakPerson(identer = setOf(Personident(ident = "12345678910")))
        val fagsak = Fagsak(fagsakPersonId = person.id, stønadstype = stønadstype, eksternId = 1001)
        val behandling =
            Behandling(
                fagsakId = fagsak.id,
                forrigeBehandlingId = if (revurdering) UUID.randomUUID() else null,
                status = BehandlingStatus.UTREDES,
                resultat = BehandlingResultat.IKKE_SATT,
            )
        val requests = mutableListOf<LagOppgaveRequest>()
        val oppgaver = mutableListOf<Oppgave>()
        var sisteOppgave: Oppgave? = null

        every { fagsakRepository.findByIdOrNull(fagsak.id) } returns fagsak
        every { fagsakPersonRepository.findByIdOrNull(person.id) } returns person
        every { behandlingRepository.findByIdOrNull(behandling.id) } returns behandling
        every { oppgaveClient.opprettOppgaveM2M(capture(requests)) } answers {
            val request = firstArg<LagOppgaveRequest>()
            OppgaveDto(
                id = requests.size.toLong(),
                tema = request.tema,
                oppgavetype = request.oppgavetype.name,
            )
        }
        every { oppgaveRepository.insert(capture(oppgaver)) } answers {
            firstArg<Oppgave>().also { sisteOppgave = it }
        }
        every {
            oppgaveRepository.finnSisteOppgaveForBehandling(
                behandling.id,
                listOf(OppgavetypeGRU.BEH_SAK.name, OppgavetypeGRU.BEH_VED.name),
            )
        } answers { sisteOppgave }
        every { oppgaveRepository.findByBehandlingIdAndType(behandling.id, any()) } answers {
            oppgaver.lastOrNull { it.type == secondArg<String>() }
        }
        every { oppgaveClient.hentOppgaveM2M(any()) } answers {
            val oppgave = oppgaver.single { it.gsakOppgaveId == firstArg<Long>() }
            OppgaveDto(
                id = oppgave.gsakOppgaveId,
                tema = Tema.GRU,
                oppgavetype = oppgave.type,
                tildeltEnhetsnr = "4415",
                versjon = 1,
            )
        }
        every { oppgaveClient.ferdigstillOppgave(any(), any()) } returns Unit

        service.opprettBehandleSakOppgave(behandling, "A123456", "4415")
        repeat(2) {
            service.ferdigstillOppgaveForType(behandling.id, OppgavetypeGRU.BEH_SAK)
            service.opprettBehandleVedtakOppgave(behandling.id)
            assertThat(service.hentAktivOppgavetype(behandling.id)).isEqualTo(OppgavetypeGRU.BEH_VED)
            service.ferdigstillOppgaveForType(behandling.id, OppgavetypeGRU.BEH_VED)
            service.opprettBehandleUnderkjentVedtakOppgave(behandling.id, "A123456")
            assertThat(service.hentAktivOppgavetype(behandling.id)).isEqualTo(OppgavetypeGRU.BEH_SAK)
        }
        service.ferdigstillOppgaveForType(behandling.id, OppgavetypeGRU.BEH_SAK)

        assertThat(requests.map { it.oppgavetype }).containsExactly(
            OppgavetypeGRU.BEH_SAK,
            OppgavetypeGRU.BEH_VED,
            OppgavetypeGRU.BEH_SAK,
            OppgavetypeGRU.BEH_VED,
            OppgavetypeGRU.BEH_SAK,
        )
        requests.forEach {
            assertThat(it.tema).isEqualTo(Tema.GRU)
            assertThat(it.behandlingstema).isEqualTo(behandlingstema)
            assertThat(it.behandlingstype).isNull()
            assertThat(it.tildeltEnhetsnr).isEqualTo("4415")
            assertThat(it.behandlesAvApplikasjon).isEqualTo("grunn-og-hjelpestonad")
            assertThat(it.tilordnetRessurs).isEqualTo(if (it.oppgavetype == OppgavetypeGRU.BEH_SAK) "A123456" else null)
        }
        oppgaver.forEach {
            verify(exactly = 1) { oppgaveClient.ferdigstillOppgave(it.gsakOppgaveId, 1) }
        }
    }

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
                stønadstype = StønadType.GRUNNSTØNAD,
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
                tema = Tema.GRU,
                oppgavetype = OppgavetypeGRU.BEH_SAK.name,
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
        assertThat(entitySlot.captured.type).isEqualTo(OppgavetypeGRU.BEH_SAK.name)
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
                stønadstype = StønadType.GRUNNSTØNAD,
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
                tema = Tema.GRU,
                oppgavetype = OppgavetypeGRU.BEH_SAK.name,
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
