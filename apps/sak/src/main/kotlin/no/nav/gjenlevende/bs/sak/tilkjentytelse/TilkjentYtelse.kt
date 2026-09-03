package no.nav.gjenlevende.bs.sak.tilkjentytelse

import no.nav.gjenlevende.bs.sak.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("tilkjent_ytelse")
data class TilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @MappedCollection(idColumn = "tilkjent_ytelse_id")
    val andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("andel_tilkjent_ytelse")
data class AndelTilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop")
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val kildeBehandlingId: UUID,
)
