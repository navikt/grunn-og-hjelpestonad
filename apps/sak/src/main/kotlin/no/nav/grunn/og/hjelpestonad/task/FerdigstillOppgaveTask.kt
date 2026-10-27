package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.spring.TaskService
import efterlatte.prosessering.strengType
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID

@Service
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val objectMapper: ObjectMapper,
) : TaskStep<String> {
    override val type: TaskType<String> = TYPE
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utfor(kontekst: TaskKontekst<String>) {
        val data: FerdigstillOppgaveTaskData = objectMapper.readValue(kontekst.payload)
        logger.info("Ferdigstiller oppgave for behandling=${data.behandlingId}, type=${data.oppgavetype}")
        oppgaveService.ferdigstillOppgaveForType(
            behandlingId = data.behandlingId,
            oppgavetype = data.oppgavetype,
        )
    }

    companion object {
        val TYPE: TaskType<String> = strengType("FerdigstillOppgaveTask")

        fun opprettTask(
            behandlingId: UUID,
            oppgavetype: OppgavetypeEYO,
            objectMapper: ObjectMapper,
            taskService: TaskService,
        ) {
            val payload =
                FerdigstillOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                )
            taskService.opprett(TYPE, objectMapper.writeValueAsString(payload))
        }
    }
}

data class FerdigstillOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val unik: LocalDateTime = LocalDateTime.now(),
)
