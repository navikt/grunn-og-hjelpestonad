package no.nav.grunn.og.hjelpestonad.beslutter.dto

import no.nav.grunn.og.hjelpestonad.beslutter.ÅrsakUnderkjent

data class BeslutteVedtakDto(
    val godkjent: Boolean,
    val årsakUnderkjent: ÅrsakUnderkjent? = null,
    val begrunnelse: String? = null,
)
