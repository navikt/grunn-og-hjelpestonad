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
    taskStepType = OpprettOppgaveTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter oppgave i Gosys",
)
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val objectMapper: ObjectMapper,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val data: OpprettOppgaveTaskData = objectMapper.readValue(task.payload)
        logger.info("Oppretter oppgave for behandling=${data.behandlingId}, type=${data.oppgavetype}")

        when (data.oppgavetype) {
            OppgavetypeEYO.GOD_VED -> {
                oppgaveService.opprettGodkjennVedtakOppgave(data.behandlingId)
            }

            OppgavetypeEYO.BEH_UND_VED -> {
                oppgaveService.opprettBehandleUnderkjentVedtakOppgave(
                    behandlingId = data.behandlingId,
                    tilordnetSaksbehandler =
                        data.tilordnetSaksbehandler
                            ?: throw IllegalStateException("tilordnetSaksbehandler påkrevd for BEH_UND_VED"),
                )
            }

            else -> {
                throw IllegalStateException("Ugyldig oppgavetype: ${data.oppgavetype}")
            }
        }
    }

    companion object {
        const val TYPE = "OpprettOppgaveTask"

        fun opprettTask(
            behandlingId: UUID,
            oppgavetype: OppgavetypeEYO,
            tilordnetSaksbehandler: String? = null,
            objectMapper: ObjectMapper,
            taskService: TaskService,
        ) {
            val payload =
                OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                    tilordnetSaksbehandler = tilordnetSaksbehandler,
                )
            val task = Task(TYPE, objectMapper.writeValueAsString(payload))
            taskService.save(task)
        }
    }
}

data class OpprettOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val tilordnetSaksbehandler: String? = null,
    val unik: LocalDateTime = LocalDateTime.now(),
)
