package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.sak.SaktreffResponse

data class InfotrygdFinnesResponse(
    val vedtak: List<VedtakstreffResponse>,
    val saker: List<SaktreffResponse>,
)

data class VedtakstreffResponse(
    val personident: String,
    val stønadstype: Stønadstype,
    val harLøpendeVedtak: Boolean,
)

data class PersonerForMigreringResponse(
    val personidenter: Set<String>,
)
