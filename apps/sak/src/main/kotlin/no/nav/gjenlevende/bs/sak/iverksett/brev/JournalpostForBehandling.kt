package no.nav.gjenlevende.bs.sak.iverksett.brev

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("journalpost_for_behandling")
data class JournalpostForBehandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val journalpostId: String,
)
