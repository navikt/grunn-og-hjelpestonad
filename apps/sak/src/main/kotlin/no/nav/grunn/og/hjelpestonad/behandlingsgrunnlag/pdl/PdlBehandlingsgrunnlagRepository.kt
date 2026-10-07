package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PdlFolkeregisterpersonstatusRepository :
    RepositoryInterface<PdlFolkeregisterpersonstatus, UUID>,
    InsertUpdateRepository<PdlFolkeregisterpersonstatus> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlFolkeregisterpersonstatus>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_folkeregisterpersonstatus WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlBostedsadresseRepository :
    RepositoryInterface<PdlBostedsadresse, UUID>,
    InsertUpdateRepository<PdlBostedsadresse> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlBostedsadresse>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_bostedsadresse WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlStatsborgerskapRepository :
    RepositoryInterface<PdlStatsborgerskap, UUID>,
    InsertUpdateRepository<PdlStatsborgerskap> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlStatsborgerskap>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_statsborgerskap WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlOppholdRepository :
    RepositoryInterface<PdlOpphold, UUID>,
    InsertUpdateRepository<PdlOpphold> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlOpphold>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_opphold WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlInnflyttingTilNorgeRepository :
    RepositoryInterface<PdlInnflyttingTilNorge, UUID>,
    InsertUpdateRepository<PdlInnflyttingTilNorge> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlInnflyttingTilNorge>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_innflytting_til_norge WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlUtflyttingFraNorgeRepository :
    RepositoryInterface<PdlUtflyttingFraNorge, UUID>,
    InsertUpdateRepository<PdlUtflyttingFraNorge> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlUtflyttingFraNorge>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_utflytting_fra_norge WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlOppholdsadresseRepository :
    RepositoryInterface<PdlOppholdsadresse, UUID>,
    InsertUpdateRepository<PdlOppholdsadresse> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlOppholdsadresse>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_oppholdsadresse WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}

@Repository
interface PdlDoedsfallRepository :
    RepositoryInterface<PdlDoedsfall, UUID>,
    InsertUpdateRepository<PdlDoedsfall> {
    fun findByBehandlingId(behandlingId: UUID): List<PdlDoedsfall>

    @Modifying
    @Query("DELETE FROM behandlingsgrunnlag_pdl_doedsfall WHERE behandling_id = :behandlingId")
    fun slettForBehandling(behandlingId: UUID)
}
