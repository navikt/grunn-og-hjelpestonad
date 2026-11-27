package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
) : TaskStep<OpprettOppgaveTaskData> {
    override val type = TYPE
    override val retry = retryMedVentetider(ventetider = emptyList())

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun utfor(kontekst: TaskKontekst<OpprettOppgaveTaskData>) {
        val data = kontekst.payload
        logger.info("Oppretter oppgave for behandling=${data.behandlingId}, type=${data.oppgavetype}")

        when (data.oppgavetype) {
            OppgavetypeGRU.BEH_VED -> {
                oppgaveService.opprettBehandleVedtakOppgave(
                    behandlingId = data.behandlingId,
                    idempotensUuid = data.idempotensUuid,
                )
            }

            OppgavetypeGRU.BEH_SAK -> {
                oppgaveService.opprettBehandleUnderkjentVedtakOppgave(
                    behandlingId = data.behandlingId,
                    tilordnetSaksbehandler =
                        data.tilordnetSaksbehandler
                            ?: throw IllegalStateException("tilordnetSaksbehandler påkrevd for BEH_SAK"),
                    idempotensUuid = data.idempotensUuid,
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
    val oppgavetype: OppgavetypeGRU,
    val tilordnetSaksbehandler: String? = null,
    val idempotensUuid: UUID = UUID.randomUUID(),
)
