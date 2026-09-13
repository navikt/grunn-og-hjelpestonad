package no.nav.grunn.og.hjelpestonad.behandling.årsak

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.util.UUID
import kotlin.jvm.JvmName

data class ÅrsakBehandlingDto(
    val behandlingId: UUID,
    val kravdato: LocalDate,
    @JsonProperty("årsak")
    @get:JvmName("getÅrsak")
    val årsak: Årsak,
    val beskrivelse: String,
)

fun ÅrsakBehandling.tilDto() =
    ÅrsakBehandlingDto(
        behandlingId = this.behandlingId,
        kravdato = this.kravdato,
        årsak = this.årsak,
        beskrivelse = this.beskrivelse,
    )
