package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.strengType
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Distribusjonstype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Fagsystem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
class DistribuerVedtaksbrevTask(
    private val objectMapper: ObjectMapper,
    private val journalpostForBehandlingService: JournalpostForBehandlingService,
    private val dokdistClient: DokdistClient,
) : TaskStep<String> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type: TaskType<String> = TYPE

    override fun utfor(kontekst: TaskKontekst<String>) {
        val taskData = objectMapper.readValue<DistribuerVedtaksbrevTaskData>(kontekst.payload)
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
                    dokumentProdApp = "GRUNN_OG_HJELPESTONAD",
                    distribusjonstype = Distribusjonstype.VEDTAK,
                )
            dokdistClient.distribuerDokument(request)
        }
    }

    companion object {
        val TYPE: TaskType<String> = strengType("distribuerVedtaksbrev")
    }

    data class DistribuerVedtaksbrevTaskData(
        val behandlingId: UUID,
    )
}
