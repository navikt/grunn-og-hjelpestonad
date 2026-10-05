package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class Behandlingsgrunnlagskilde {
    PDL,
    MEDL,
}

@Table("behandlingsgrunnlag_henting")
data class BehandlingsgrunnlagHenting(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val kilde: Behandlingsgrunnlagskilde,
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)

@Repository
interface BehandlingsgrunnlagHentingRepository :
    RepositoryInterface<BehandlingsgrunnlagHenting, UUID>,
    InsertUpdateRepository<BehandlingsgrunnlagHenting> {
    fun findByBehandlingIdAndKilde(
        behandlingId: UUID,
        kilde: Behandlingsgrunnlagskilde,
    ): BehandlingsgrunnlagHenting?

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_henting WHERE behandling_id = :behandlingId AND kilde = :kilde")
    fun slett(
        behandlingId: UUID,
        kilde: Behandlingsgrunnlagskilde,
    )
}
