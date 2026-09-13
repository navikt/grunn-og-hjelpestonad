package no.nav.grunn.og.hjelpestonad.fagsak.dto

import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import java.util.UUID

data class FagsakResponse(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personident: String,
    val stønadstype: StønadType,
    val eksternId: Long,
)

fun Fagsak.tilResponse(personident: String): FagsakResponse =
    FagsakResponse(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personident = personident,
        stønadstype = this.stønadstype,
        eksternId = this.eksternId,
    )
