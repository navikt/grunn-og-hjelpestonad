package no.nav.grunn.og.hjelpestonad.oppgave

import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("oppgave")
data class Oppgave(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Column("gsak_oppgave_id")
    val gsakOppgaveId: Long,
    val type: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
