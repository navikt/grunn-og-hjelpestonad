package no.nav.grunn.og.hjelpestonad.iverksett.brev

import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class IverksettVedtaksbrevService(
    private val objectMapper: ObjectMapper,
) {
    fun opprettIverksettVedtaksbrevTask(behandlingId: UUID): String =
        objectMapper.writeValueAsString(
            JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData(behandlingId),
        )

    fun opprettDistribuerVedtaksbrevTask(behandlingId: UUID): String =
        objectMapper.writeValueAsString(
            DistribuerVedtaksbrevTask.DistribuerVedtaksbrevTaskData(behandlingId),
        )
}
