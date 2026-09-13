package no.nav.grunn.og.hjelpestonad.behandling.årsak

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID
import kotlin.jvm.JvmName

data class ÅrsakBehandlingResponse(
    val behandlingId: UUID,
    val kravdato: LocalDate,
    @JsonProperty("årsak")
    @get:JvmName("getÅrsak")
    val årsak: Årsak,
    val beskrivelse: String,
)

fun ÅrsakBehandling.tilResponse() =
    ÅrsakBehandlingResponse(
        behandlingId = this.behandlingId,
        kravdato = this.kravdato,
        årsak = this.årsak,
        beskrivelse = this.beskrivelse,
    )
