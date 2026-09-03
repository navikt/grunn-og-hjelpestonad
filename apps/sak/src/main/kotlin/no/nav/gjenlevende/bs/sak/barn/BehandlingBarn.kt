package no.nav.gjenlevende.bs.sak.barn

import no.nav.gjenlevende.bs.sak.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val personIdent: String,
    val navn: String,
    @Column("fodsel_dato")
    val fødselsdato: LocalDate,
    val hentetTidspunkt: LocalDateTime,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
