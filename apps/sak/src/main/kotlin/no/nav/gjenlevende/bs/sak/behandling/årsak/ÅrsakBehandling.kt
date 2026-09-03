package no.nav.gjenlevende.bs.sak.behandling.årsak

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

enum class Årsak {
    SØKNAD,
    NYE_OPPLYSNINGER,
    ANNET,
}

@Table("arsak_behandling")
data class ÅrsakBehandling(
    @Id
    val behandlingId: UUID,
    val kravdato: LocalDate,
    @Column("arsak")
    val årsak: Årsak,
    val beskrivelse: String = "",
)
