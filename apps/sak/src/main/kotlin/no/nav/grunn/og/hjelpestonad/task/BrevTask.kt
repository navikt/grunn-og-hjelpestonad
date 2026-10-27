package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.brev.FamilieDokumentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class BrevTask(
    val brevService: BrevService,
    private val familieDokumentClient: FamilieDokumentClient,
) : TaskStep<BrevTask.LagBrevPdfTaskData> {
    override val type: TaskType<LagBrevPdfTaskData> = TYPE
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun utfor(kontekst: TaskKontekst<LagBrevPdfTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
        val brev =
            brevService.hentBrev(behandlingId)
                ?: error("Fant ikke brev for behandlingId=$behandlingId")
        val html = brevService.lagHtml(brev)
        val pdf = familieDokumentClient.genererPdfFraHtml(html)
        brevService.oppdatereBrevPdf(behandlingId, pdf)

        logger.info("Gjennomført BrevTask: behandlingId={}", behandlingId)
    }

    companion object {
        val TYPE: TaskType<LagBrevPdfTaskData> = lagTaskType("BrevTask")
    }

    data class LagBrevPdfTaskData(
        val behandlingId: UUID,
        // Trengs fordi prosessering_task har UNIQUE (type, payload), og samme behandling kan få ny brev-task.
        val unik: LocalDateTime = LocalDateTime.now(),
    )
}
