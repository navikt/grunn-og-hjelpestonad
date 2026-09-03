package no.nav.gjenlevende.bs.sak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.gjenlevende.bs.sak.brev.BrevService
import no.nav.gjenlevende.bs.sak.brev.FamilieDokumentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BrevTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Brev task",
)
class BrevTask(
    val brevService: BrevService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val objectMapper: ObjectMapper,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val data = objectMapper.readValue(task.payload, LagBrevPdfTaskData::class.java)
        val behandlingId = data.behandlingId
        val brev =
            brevService.hentBrev(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId")
        val html = brevService.lagHtml(brev)
        val pdf = familieDokumentClient.genererPdfFraHtml(html)
        brevService.oppdatereBrevPdf(behandlingId, pdf)

        logger.info("Gjennomført BrevTask: behandlingId={}", behandlingId)
    }

    companion object {
        const val TYPE = "BrevTask"

        fun opprettTask(payload: String): Task =
            Task(
                TYPE,
                payload,
            )
    }

    data class LagBrevPdfTaskData(
        val behandlingId: UUID,
        val unik: LocalDateTime = LocalDateTime.now(),
    )
}
