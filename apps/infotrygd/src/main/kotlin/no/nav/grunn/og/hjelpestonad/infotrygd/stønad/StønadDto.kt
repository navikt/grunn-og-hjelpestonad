package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.sak.Saktreff

data class InfotrygdFinnesResponse(
    val vedtak: List<Vedtakstreff>,
    val saker: List<Saktreff>,
)

data class Vedtakstreff(
    val personident: String,
    val stønadstype: Stønadstype,
    val harLøpendeVedtak: Boolean,
)
