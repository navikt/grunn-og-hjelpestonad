package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.fagsak.domain.StønadType
import java.util.UUID

data class FagsakRequest(
    val personident: String?,
    val fagsakPersonId: UUID?,
    val stønadstype: StønadType,
)
