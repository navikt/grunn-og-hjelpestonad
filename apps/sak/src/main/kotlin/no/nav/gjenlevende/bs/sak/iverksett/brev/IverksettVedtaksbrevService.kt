package no.nav.gjenlevende.bs.sak.iverksett.brev

import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class IverksettVedtaksbrevService(
    private val objectMapper: ObjectMapper,
) {
    fun opprettIverksettVedtaksbrevTask(behandlingId: UUID): Task =
        JournalførVedtaksbrevTask.opprettTask(
            objectMapper.writeValueAsString(
                JournalførVedtaksbrevTask.JournalførVedtaksbrevTaskData(behandlingId),
            ),
        )

    fun opprettDistribuerVedtaksbrevTask(behandlingId: UUID): Task =
        DistribuerVedtaksbrevTask.opprettTask(
            objectMapper.writeValueAsString(
                DistribuerVedtaksbrevTask.DistribuerVedtaksbrevTaskData(behandlingId),
            ),
        )
}
