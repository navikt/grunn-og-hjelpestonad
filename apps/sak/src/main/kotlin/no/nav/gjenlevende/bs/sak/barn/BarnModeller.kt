package no.nav.gjenlevende.bs.sak.barn

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class HentBarnRequest(
    val personIdent: String,
    val behandlingId: UUID,
)

data class HentBarnResponse(
    val id: UUID,
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate,
    val hentetTidspunkt: LocalDateTime,
)
