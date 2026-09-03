package no.nav.gjenlevende.bs.sak.brev.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("brevmottaker")
data class Brevmottaker(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val personRolle: BrevmottakerRolle,
    val mottakerType: MottakerType,
    val personident: String? = null,
    val orgnr: String? = null,
    val navnHosOrganisasjon: String? = null,
)

enum class BrevmottakerRolle {
    BRUKER,
    VERGE,
    FULLMEKTIG,
}

enum class MottakerType {
    PERSON,
    ORGANISASJON,
}
