package no.nav.gjenlevende.bs.sak.fagsak.domain

import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.felles.sporbar.Sporbar
import no.nav.gjenlevende.bs.sak.felles.sporbar.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDateTime
import java.util.UUID

data class FagsakPerson(
    @Id
    val id: UUID = UUID.randomUUID(),
    @MappedCollection(idColumn = "fagsak_person_id")
    val identer: Set<Personident>,
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
)

data class Personident(
    @Id
    val ident: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
