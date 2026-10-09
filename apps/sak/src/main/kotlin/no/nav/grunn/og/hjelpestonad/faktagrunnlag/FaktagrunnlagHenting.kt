package no.nav.grunn.og.hjelpestonad.faktagrunnlag

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

enum class Faktagrunnlagskilde {
    PDL,
    MEDL,
}

@Table("faktagrunnlag_henting")
data class FaktagrunnlagHenting(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val kilde: Faktagrunnlagskilde,
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)

@Repository
interface FaktagrunnlagHentingRepository :
    RepositoryInterface<FaktagrunnlagHenting, UUID>,
    InsertUpdateRepository<FaktagrunnlagHenting> {
    fun findByBehandlingIdAndKilde(
        behandlingId: UUID,
        kilde: Faktagrunnlagskilde,
    ): FaktagrunnlagHenting?

    @Modifying
    @Query("DELETE FROM faktagrunnlag_henting WHERE behandling_id = :behandlingId AND kilde = :kilde")
    fun slett(
        behandlingId: UUID,
        kilde: Faktagrunnlagskilde,
    )
}
