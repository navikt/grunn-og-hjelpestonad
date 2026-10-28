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
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
) : TaskStep<FerdigstillOppgaveTaskData> {
    override val type = TYPE
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
        val TYPE = lagTaskType<FerdigstillOppgaveTaskData>("FerdigstillOppgaveTask")
    }
}

data class FerdigstillOppgaveTaskData(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeEYO,
    val unik: LocalDateTime = LocalDateTime.now(),
)
