package no.nav.grunn.og.hjelpestonad.behandling

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.spring.TaskService
import efterlatte.prosessering.strengType
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.saksbehandler.EntraProxyClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
class LagBehandleSakOppgaveTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val objectMapper: ObjectMapper,
    private val taskService: TaskService,
    private val entraProxyClient: EntraProxyClient,
) : TaskStep<String> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type: TaskType<String> = TYPE

    override fun utfor(kontekst: TaskKontekst<String>) {
        val payload: OpprettOppgavePayload = objectMapper.readValue(kontekst.payload)
        logger.info("LagBehandleSakOppgaveTask task: ${payload.behandlingsId}, saksbehandler: ${payload.saksbehandler}")
        val behandling = behandlingService.hentBehandling(payload.behandlingsId) ?: throw IllegalStateException("Finner ikke behandling med id=${payload.behandlingsId}")
        oppgaveService.opprettBehandleSakOppgave(behandling, payload.saksbehandler, payload.tildeltEnhetsnr)
    }

    fun opprettBehandleSakOppgaveTask(
        behandling: Behandling,
        saksbehandler: String,
    ) {
        val enhetnummer = entraProxyClient.hentSaksbehandlerInfo(saksbehandler).enhet.enhetnummer
        val payload = OpprettOppgavePayload(behandlingsId = behandling.id, saksbehandler = saksbehandler, tildeltEnhetsnr = enhetnummer)
        val payloadAsString = objectMapper.writeValueAsString(payload)
        taskService.opprett(TYPE, payloadAsString)
    }

    companion object {
        val TYPE: TaskType<String> = strengType("LagBehandleSakOppgaveTask")
    }
}

data class OpprettOppgavePayload(
    val behandlingsId: UUID,
    val saksbehandler: String,
    val uniqueTaskPayloadId: UUID = UUID.randomUUID(),
    val tildeltEnhetsnr: String,
)
