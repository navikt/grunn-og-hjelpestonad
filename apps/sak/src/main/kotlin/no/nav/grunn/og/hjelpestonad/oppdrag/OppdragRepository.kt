package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.felles.InsertUpdateRepository
import no.nav.grunn.og.hjelpestonad.felles.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/** Status på oppdraget. Verdiene fra OS er navnene på `OppdragStatus` i familie-kontrakter. */
object OppdragStatus {
    /** Lagret, men ikke bekreftet sendt. Et nytt forsøk sender det lagrede oppdraget. */
    const val OPPRETTET = "OPPRETTET"
    const val LAGT_PÅ_KØ = "LAGT_PÅ_KØ"
    const val KVITTERT_OK = "KVITTERT_OK"

    /** Generatoren fant ingen endring mot forrige behandling. Oppdraget sendes ikke, men andelene lagres. */
    const val INGEN_ENDRING = "INGEN_ENDRING"

    /** Statuser der OS har det samme som `utbetalingsandel`, slik at neste behandling kan kjedes mot andelene. */
    val FERDIG = setOf(KVITTERT_OK, INGEN_ENDRING)
}

@Table("oppdrag_resultat")
data class OppdragResultat(
    @Id
    val behandlingId: UUID,
    val fagsakId: UUID,
    val utbetalingsoppdrag: Utbetalingsoppdrag,
    val status: String,
    val kvitteringsmelding: String? = null,
    @MappedCollection(idColumn = "behandling_id")
    val andeler: Set<Utbetalingsandel>,
)

/** En løpende andel etter en behandling, slik den er sendt til OS. Se ADR-0011. */
@Table("utbetalingsandel")
data class Utbetalingsandel(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fom: LocalDate,
    val tom: LocalDate,
    @Column("belop")
    val beløp: Int,
    val ytelsestype: GrunnOgHjelpYtelsestype,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val kildeBehandlingId: UUID,
)

@Repository
interface OppdragRepository :
    RepositoryInterface<OppdragResultat, UUID>,
    InsertUpdateRepository<OppdragResultat> {
    /** Låser fagsaken til transaksjonen er ferdig så to behandlinger ikke kjedes samtidig. Feiler uten transaksjon fordi låsen ellers slippes med en gang. */
    @Query("SELECT id FROM fagsak WHERE id = :fagsakId FOR UPDATE")
    @Transactional(propagation = Propagation.MANDATORY)
    fun låsFagsak(fagsakId: UUID): UUID?

    /** Andelene til siste behandling på fagsaken som har et oppdrag, også når den ikke endret noe. */
    @Query(
        """
        SELECT ua.* FROM utbetalingsandel ua
        WHERE ua.behandling_id = (
            SELECT o.behandling_id FROM oppdrag_resultat o
            WHERE o.fagsak_id = :fagsakId AND o.behandling_id <> :unntattBehandlingId
            ORDER BY o.opprettet_tid DESC
            LIMIT 1
        )
        ORDER BY ua.fom
        """,
    )
    fun finnForrigeAndeler(
        fagsakId: UUID,
        unntattBehandlingId: UUID,
    ): List<Utbetalingsandel>

    /**
     * Siste andel i hver kjede: høyeste `periode_id` per ytelsestype på fagsaken. Finnes samme `periode_id` i flere
     * behandlinger, er det den første lagrede som gjelder, fordi en senere behandling kan ha avkortet `tom`.
     * Virker også når siste behandling opphørte alle andelene.
     */
    @Query(
        """
         SELECT id, behandling_id, fom, tom, belop, ytelsestype, periode_id, forrige_periode_id, kilde_behandling_id
        FROM (
            SELECT ua.*, ROW_NUMBER() OVER (
                 PARTITION BY ua.ytelsestype ORDER BY ua.periode_id DESC, o.opprettet_tid ASC
            ) AS rn
            FROM utbetalingsandel ua
            JOIN oppdrag_resultat o ON o.behandling_id = ua.behandling_id
            WHERE o.fagsak_id = :fagsakId AND o.behandling_id <> :unntattBehandlingId
        ) siste WHERE rn = 1
        """,
    )
    fun finnSisteAndelPerKjede(
        fagsakId: UUID,
        unntattBehandlingId: UUID,
    ): List<Utbetalingsandel>

    /** Ikke bruk update() på aggregatet, det sletter og setter inn andelene på nytt. */
    @Modifying
    @Query("UPDATE oppdrag_resultat SET status = :status, kvitteringsmelding = :kvitteringsmelding, endret_tid = LOCALTIMESTAMP WHERE behandling_id = :behandlingId")
    fun oppdaterStatus(
        behandlingId: UUID,
        status: String,
        kvitteringsmelding: String?,
    )
}
