package no.nav.grunn.og.hjelpestonad.behandling

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.spring.TaskService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.saksbehandler.EntraProxyClient
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LagBehandleSakOppgaveTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val entraProxyClient: EntraProxyClient,
) : TaskStep<OpprettOppgavePayload> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE

    override fun utfor(kontekst: TaskKontekst<OpprettOppgavePayload>) {
        logger.info("LagBehandleSakOppgaveTask task: ${kontekst.payload.behandlingsId}, saksbehandler: ${kontekst.payload.saksbehandler}")
        oppgaveService.opprettBehandleSakOppgave(
            behandling = behandlingService.hentBehandlingEllerKast(kontekst.payload.behandlingsId),
            saksbehandler = kontekst.payload.saksbehandler,
            tildeltEnhetsnr = kontekst.payload.tildeltEnhetsnr,
        )
    }

    fun opprettBehandleSakOppgaveTask(
        behandling: Behandling,
        saksbehandler: String,
    ) {
        val payload =
            OpprettOppgavePayload(
                behandlingsId = behandling.id,
                saksbehandler = saksbehandler,
                tildeltEnhetsnr = entraProxyClient.hentSaksbehandlerInfo(saksbehandler).enhet.enhetnummer,
            )
        taskService.opprett(TYPE, payload)
    }

    companion object {
        val TYPE = lagTaskType<OpprettOppgavePayload>("LagBehandleSakOppgaveTask")
    }
}

data class OpprettOppgavePayload(
    val behandlingsId: UUID,
    val saksbehandler: String,
    val uniqueTaskPayloadId: UUID = UUID.randomUUID(),
    val tildeltEnhetsnr: String,
)
