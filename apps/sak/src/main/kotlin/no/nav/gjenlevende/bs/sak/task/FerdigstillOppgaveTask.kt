package no.nav.gjenlevende.bs.sak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.oppgave.OppgaveService
import no.nav.gjenlevende.bs.sak.oppgave.OppgavetypeEYO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaveTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    beskrivelse = "Ferdigstiller oppgave i Gosys",
)
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val objectMapper: ObjectMapper,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val data: FerdigstillOppgaveTaskData = objectMapper.readValue(task.payload)
        logger.info("Ferdigstiller oppgave for behandling=${data.behandlingId}, type=${data.oppgavetype}")
        oppgaveService.ferdigstillOppgaveForType(
            behandlingId = data.behandlingId,
            oppgavetype = data.oppgavetype,
        )
    }

    companion object {
        const val TYPE = "FerdigstillOppgaveTask"

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
            val task = Task(TYPE, objectMapper.writeValueAsString(payload))
            taskService.save(task)
        }
    }
}

data class FerdigstillOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val unik: LocalDateTime = LocalDateTime.now(),
)
