package no.nav.grunn.og.hjelpestonad.behandling

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.spring.TaskService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.saksbehandler.EntraProxyClient
import org.junit.jupiter.api.Test
import java.util.UUID

class LagBehandleSakOppgaveTaskTest {
    private val behandlingService = mockk<BehandlingService>()
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val taskService = mockk<TaskService>()
    private val entraProxyClient = mockk<EntraProxyClient>()
    private val task = LagBehandleSakOppgaveTask(behandlingService, oppgaveService, taskService, entraProxyClient)
    private val behandling =
        Behandling(fagsakId = UUID.randomUUID(), status = BehandlingStatus.OPPRETTET, resultat = BehandlingResultat.IKKE_SATT)

    @Test
    fun `omkjøring av lagret payload sender samme idempotensUuid til tjenesten`() {
        val idempotensUuid = UUID.randomUUID()
        val lagretPayload =
            """{"behandlingsId":"${behandling.id}","saksbehandler":"A123456","tildeltEnhetsnr":"4415","idempotensUuid":"$idempotensUuid"}"""
        every { behandlingService.hentBehandlingEllerKast(behandling.id) } returns behandling

        task.utfor(kontekst(LagBehandleSakOppgaveTask.TYPE.deserialiser(lagretPayload)))
        task.utfor(kontekst(LagBehandleSakOppgaveTask.TYPE.deserialiser(lagretPayload)))

        verify(exactly = 2) {
            oppgaveService.opprettBehandleSakOppgave(
                behandling = behandling,
                saksbehandler = "A123456",
                tildeltEnhetsnr = "4415",
                idempotensUuid = idempotensUuid,
            )
        }
    }

    private fun kontekst(data: OpprettOppgavePayload) =
        mockk<TaskKontekst<OpprettOppgavePayload>> {
            every { payload } returns data
        }
}
