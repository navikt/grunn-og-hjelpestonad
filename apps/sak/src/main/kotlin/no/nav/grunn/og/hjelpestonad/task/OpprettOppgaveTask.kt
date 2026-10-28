package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
) : TaskStep<OpprettOppgaveTaskData> {
    override val type = TYPE
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
        val TYPE = lagTaskType<OpprettOppgaveTaskData>("OpprettOppgaveTask")
    }
}

data class OpprettOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val tilordnetSaksbehandler: String? = null,
    val unik: LocalDateTime = LocalDateTime.now(),
)
