package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType

data class FagsakRequest(
    val personident: String,
    val stønadstype: StønadType,
)
