package no.nav.grunn.og.hjelpestonad.iverksett.brev

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IverksettVedtaksbrevService {
    fun opprettIverksettVedtaksbrevTask(
        behandlingId: UUID,
    ): JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData = JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData(behandlingId)

    fun opprettDistribuerVedtaksbrevTask(
        behandlingId: UUID,
    ): DistribuerVedtaksbrevTask.DistribuerVedtaksbrevTaskData = DistribuerVedtaksbrevTask.DistribuerVedtaksbrevTaskData(behandlingId)
}
