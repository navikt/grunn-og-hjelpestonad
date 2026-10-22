package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderRequest

data class InfotrygdStonaderRequest(
    val personident: String,
    val stønadstype: StonaderRequest.Stønadstype,
)
