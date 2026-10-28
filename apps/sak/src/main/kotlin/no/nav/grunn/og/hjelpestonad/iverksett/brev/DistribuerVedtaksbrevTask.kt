package no.nav.grunn.og.hjelpestonad.iverksett.brev

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.DistribuerJournalpostRequest
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Distribusjonstype
import no.nav.grunn.og.hjelpestonad.iverksett.brev.domene.Fagsystem
import no.nav.grunn.og.hjelpestonad.task.lagTaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DistribuerVedtaksbrevTask(
    private val journalpostForBehandlingService: JournalpostForBehandlingService,
    private val dokdistClient: DokdistClient,
) : TaskStep<DistribuerVedtaksbrevTask.DistribuerVedtaksbrevTaskData> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override val type = TYPE

    override fun utfor(kontekst: TaskKontekst<DistribuerVedtaksbrevTaskData>) {
        val behandlingId = kontekst.payload.behandlingId
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
        val TYPE = lagTaskType<DistribuerVedtaksbrevTaskData>("distribuerVedtaksbrev")
    }

    data class DistribuerVedtaksbrevTaskData(
        val behandlingId: UUID,
    )
}
