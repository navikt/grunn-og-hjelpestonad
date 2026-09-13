package no.nav.grunn.og.hjelpestonad.beslutter.dto

import no.nav.grunn.og.hjelpestonad.beslutter.ÅrsakUnderkjent
import kotlin.jvm.JvmName

data class BeslutteVedtakDto(
    val godkjent: Boolean,
    @get:JvmName("getÅrsakUnderkjent")
    val årsakUnderkjent: ÅrsakUnderkjent? = null,
    val begrunnelse: String? = null,
)
