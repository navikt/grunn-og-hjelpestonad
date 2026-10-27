package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.strengType
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.brev.FamilieDokumentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@Service
class BrevTask(
    val brevService: BrevService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val objectMapper: ObjectMapper,
) : TaskStep<String> {
    override val type: TaskType<String> = TYPE
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun utfor(kontekst: TaskKontekst<String>) {
        val data = objectMapper.readValue(kontekst.payload, LagBrevPdfTaskData::class.java)
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
        val TYPE: TaskType<String> = strengType("BrevTask")
    }

    data class LagBrevPdfTaskData(
        val behandlingId: UUID,
        val unik: LocalDateTime = LocalDateTime.now(),
    )
}
