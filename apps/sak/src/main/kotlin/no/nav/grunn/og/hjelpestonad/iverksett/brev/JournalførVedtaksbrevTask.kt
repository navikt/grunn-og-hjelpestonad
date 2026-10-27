package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalførVedtaksbrevTask(
    private val dokarkivClient: DokarkivClient,
    private val journalføringService: JournalføringService,
    private val journalpostForBehandlingService: JournalpostForBehandlingService,
) : TaskStep<JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type: TaskType<JournalførVedtaksbrevTaskData> = TYPE

    override fun utfor(kontekst: TaskKontekst<JournalførVedtaksbrevTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val journalføringRequester = journalføringService.lagJournalføringRequester(behandlingId)

        journalføringRequester.forEach { request ->
            val response = dokarkivClient.arkiverDokument(request, true)
            journalpostForBehandlingService.lagreJournalpostId(behandlingId, response.journalpostId)
            logger.info("Journalført vedtaksbrev for mottaker ${request.avsenderMottaker?.navn}: journalpostId=${response.journalpostId}")
        }
    }

    companion object {
        val TYPE: TaskType<JournalførVedtaksbrevTaskData> = lagTaskType("journalførVedtaksbrev")
    }

    data class JournalførVedtaksbrevTaskData(
        val behandlingId: UUID,
    )
}
