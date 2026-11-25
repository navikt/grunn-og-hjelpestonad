package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

/**
 * Lagrer simuleringen per behandling i tabellen `simulering`. Én rad per behandling: en ny simulering erstatter den forrige.
 *
 * `respons` er svaret fra Oppdrag, med fødselsnummer. Det lagres og logges ikke her.
 */
@Repository
class SimuleringRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun lagre(
        behandlingId: UUID,
        status: Simuleringsstatus,
        respons: JsonNode?,
        opprettetTid: LocalDateTime,
    ) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO simulering (behandling_id, status, respons, opprettet_tid)
            VALUES (:behandlingId, :status, CAST(:respons AS JSONB), :opprettetTid)
            ON CONFLICT (behandling_id) DO UPDATE SET
                status = EXCLUDED.status,
                respons = EXCLUDED.respons,
                opprettet_tid = EXCLUDED.opprettet_tid
            """.trimIndent(),
            mapOf(
                "behandlingId" to behandlingId,
                "status" to status.name,
                "respons" to respons?.let { objectMapper.writeValueAsString(it) },
                "opprettetTid" to opprettetTid,
            ),
        )
    }

    fun finn(behandlingId: UUID): LagretSimulering? =
        namedParameterJdbcTemplate
            .query(
                """
                SELECT behandling_id, status, respons, opprettet_tid
                FROM simulering
                WHERE behandling_id = :behandlingId
                """.trimIndent(),
                mapOf("behandlingId" to behandlingId),
            ) { resultSet, _ ->
                LagretSimulering(
                    behandlingId = resultSet.getObject("behandling_id", UUID::class.java),
                    status = Simuleringsstatus.valueOf(resultSet.getString("status")),
                    respons = resultSet.getString("respons")?.let { objectMapper.readTree(it) },
                    opprettetTid = resultSet.getObject("opprettet_tid", LocalDateTime::class.java),
                )
            }.singleOrNull()

    fun slett(behandlingId: UUID) {
        namedParameterJdbcTemplate.update(
            "DELETE FROM simulering WHERE behandling_id = :behandlingId",
            mapOf("behandlingId" to behandlingId),
        )
    }
}

data class LagretSimulering(
    val behandlingId: UUID,
    val status: Simuleringsstatus,
    val respons: JsonNode?,
    val opprettetTid: LocalDateTime,
)
