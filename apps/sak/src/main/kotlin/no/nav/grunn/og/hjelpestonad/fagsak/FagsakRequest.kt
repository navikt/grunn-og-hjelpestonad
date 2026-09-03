package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import java.util.UUID

data class FagsakRequest(
    val personident: String?,
    val fagsakPersonId: UUID?,
    val stønadstype: StønadType,
)
