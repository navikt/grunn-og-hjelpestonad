package no.nav.gjenlevende.bs.sak.beslutter.dto

import no.nav.gjenlevende.bs.sak.beslutter.ÅrsakUnderkjent

data class BeslutteVedtakDto(
    val godkjent: Boolean,
    val årsakUnderkjent: ÅrsakUnderkjent? = null,
    val begrunnelse: String? = null,
)
