package no.nav.gjenlevende.bs.sak.iverksett.brev

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalpostForBehandlingService(
    private val journalpostForBehandlingRepository: JournalpostForBehandlingRepository,
) {
    fun hentJournalpostIder(behandlingId: UUID): List<String> =
        journalpostForBehandlingRepository
            .findAllByBehandlingId(behandlingId)
            .map { it.journalpostId }

    fun lagreJournalpostId(
        behandlingId: UUID,
        journalpostId: String,
    ): JournalpostForBehandling =
        journalpostForBehandlingRepository.insert(
            JournalpostForBehandling(
                behandlingId = behandlingId,
                journalpostId = journalpostId,
            ),
        )
}
