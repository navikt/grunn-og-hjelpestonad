package no.nav.gjenlevende.bs.sak.beslutter.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.gjenlevende.bs.sak.beslutter.ÅrsakUnderkjent
import java.time.LocalDateTime

data class TotrinnskontrollStatusDto(
    val status: TotrinnskontrollStatus,
    val totrinnskontroll: TotrinnskontrollDto? = null,
)

data class TotrinnskontrollDto(
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val godkjent: Boolean? = null,
    @JsonProperty("årsakUnderkjent")
    val årsakUnderkjent: ÅrsakUnderkjent? = null,
    val begrunnelse: String? = null,
)

enum class TotrinnskontrollStatus {
    TOTRINNSKONTROLL_UNDERKJENT,
    KAN_FATTE_VEDTAK,
    IKKE_AUTORISERT,
    UAKTUELT,
}
