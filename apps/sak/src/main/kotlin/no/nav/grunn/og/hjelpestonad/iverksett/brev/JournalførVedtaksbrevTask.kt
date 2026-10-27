package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.strengType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
class JournalførVedtaksbrevTask(
    private val dokarkivClient: DokarkivClient,
    private val objectMapper: ObjectMapper,
    private val journalføringService: JournalføringService,
    private val journalpostForBehandlingService: JournalpostForBehandlingService,
) : TaskStep<String> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type: TaskType<String> = TYPE

    override fun utfor(kontekst: TaskKontekst<String>) {
        val taskData = objectMapper.readValue<JournalførVedtaksbrevTaskData>(kontekst.payload)
        val behandlingId = taskData.behandlingId
        val journalføringRequester = journalføringService.lagJournalføringRequester(behandlingId)

        journalføringRequester.forEach { request ->
            val response = dokarkivClient.arkiverDokument(request, true)
            journalpostForBehandlingService.lagreJournalpostId(behandlingId, response.journalpostId)
            logger.info("Journalført vedtaksbrev for mottaker ${request.avsenderMottaker?.navn}: journalpostId=${response.journalpostId}")
        }
    }

    companion object {
        val TYPE: TaskType<String> = strengType("journalførVedtaksbrev")
    }

    data class JournalførVedtaksbrevTaskData(
        val behandlingId: UUID,
    )
}
