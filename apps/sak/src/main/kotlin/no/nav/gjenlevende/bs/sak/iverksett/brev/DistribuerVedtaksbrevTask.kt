package no.nav.gjenlevende.bs.sak.iverksett.brev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Distribusjonstype
import no.nav.gjenlevende.bs.sak.iverksett.brev.domene.Fagsystem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTask.TYPE,
    maxAntallFeil = DistribuerVedtaksbrevTask.MAX_FORSØK,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Distribuerer vedtaksbrev.",
)
class DistribuerVedtaksbrevTask(
    private val objectMapper: ObjectMapper,
    private val journalpostForBehandlingService: JournalpostForBehandlingService,
    private val dokdistClient: DokdistClient,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue<DistribuerVedtaksbrevTaskData>(task.payload)
        val behandlingId = taskData.behandlingId
        distribuerVedtaksbrev(behandlingId)
    }

    fun distribuerVedtaksbrev(behandlingId: UUID) {
        val journalpostIDerSomSkalDistribueres = journalpostForBehandlingService.hentJournalpostIder(behandlingId)
        journalpostIDerSomSkalDistribueres.forEach { journalpostId ->
            val request =
                DistribuerJournalpostRequest(
                    journalpostId = journalpostId,
                    bestillendeFagsystem = Fagsystem.EY,
                    dokumentProdApp = "GJENLEVENDE_BS_SAK",
                    distribusjonstype = Distribusjonstype.VEDTAK,
                )
            dokdistClient.distribuerDokument(request)
        }
    }

    companion object {
        const val TYPE = "distribuerVedtaksbrev"
        const val MAX_FORSØK = 50

        fun opprettTask(payload: String): Task =
            Task(
                TYPE,
                payload,
            )
    }

    data class DistribuerVedtaksbrevTaskData(
        val behandlingId: UUID,
    )
}
