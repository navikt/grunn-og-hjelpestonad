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
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
) : TaskStep<OpprettOppgaveTaskData> {
    override val type: TaskType<OpprettOppgaveTaskData> = TYPE
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utfor(kontekst: TaskKontekst<OpprettOppgaveTaskData>) {
        val data = kontekst.payload
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
        val TYPE: TaskType<OpprettOppgaveTaskData> = lagTaskType("OpprettOppgaveTask")

        fun opprettTask(
            behandlingId: UUID,
            oppgavetype: OppgavetypeEYO,
            tilordnetSaksbehandler: String? = null,
            taskService: TaskService,
        ) {
            val payload =
                OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                    tilordnetSaksbehandler = tilordnetSaksbehandler,
                )
            taskService.opprett(TYPE, payload)
        }
    }
}

data class OpprettOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val tilordnetSaksbehandler: String? = null,
    // Trengs fordi prosessering_task har UNIQUE (type, payload), og samme behandling kan få samme task flere ganger.
    val unik: LocalDateTime = LocalDateTime.now(),
)
