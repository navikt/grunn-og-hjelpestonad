package no.nav.gjenlevende.bs.sak.fagsak.domain

import no.nav.gjenlevende.bs.sak.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("fagsak")
data class Fagsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakPersonId: UUID,
    val eksternId: Long = 0,
    @Column("stonadstype")
    val stønadstype: StønadType,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class StønadType(
    val kodeRutine: String,
    val behandlingstema: String,
) {
    BARNETILSYN("GB", behandlingstema = "ab0028"),
    SKOLEPENGER("GU", behandlingstema = "ab0177"),
}
