package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.spring.TaskService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
) : TaskStep<FerdigstillOppgaveTaskData> {
    override val type: TaskType<FerdigstillOppgaveTaskData> = TYPE
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utfor(kontekst: TaskKontekst<FerdigstillOppgaveTaskData>) {
        val data = kontekst.payload
        logger.info("Ferdigstiller oppgave for behandling=${data.behandlingId}, type=${data.oppgavetype}")
        oppgaveService.ferdigstillOppgaveForType(
            behandlingId = data.behandlingId,
            oppgavetype = data.oppgavetype,
        )
    }

    companion object {
        val TYPE: TaskType<FerdigstillOppgaveTaskData> = lagTaskType("FerdigstillOppgaveTask")

        fun opprettTask(
            behandlingId: UUID,
            oppgavetype: OppgavetypeEYO,
            taskService: TaskService,
        ) {
            val payload =
                FerdigstillOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                )
            taskService.opprett(TYPE, payload)
        }
    }
}

data class FerdigstillOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val unik: LocalDateTime = LocalDateTime.now(),
)
