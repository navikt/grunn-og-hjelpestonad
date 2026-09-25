package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

enum class JobStatus {
    OK,
    PAGINERER,
    NY_BASELINE,
}

data class ExodusStatus(
    val tabell: String,
    val iterator: String?,
    val jobStatus: JobStatus,
    val antallRaderHentet: Long,
    val sistOppdatert: LocalDateTime,
)

@Repository
open class ExodusStatusRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    open fun finn(tabellnavn: String): ExodusStatus? =
        jdbcTemplate
            .query(
                "SELECT * FROM exodus_status WHERE tabell = :tabell",
                MapSqlParameterSource("tabell", tabellnavn),
            ) { rs, _ ->
                ExodusStatus(
                    tabell = rs.getString("tabell"),
                    iterator = rs.getString("iterator"),
                    jobStatus = JobStatus.valueOf(rs.getString("job_status")),
                    antallRaderHentet = rs.getLong("antall_rader_hentet"),
                    sistOppdatert = rs.getTimestamp("sist_oppdatert").toLocalDateTime(),
                )
            }.firstOrNull()

    open fun oppdaterIterator(
        tabellnavn: String,
        iterator: String?,
        antallNyeRader: Int,
        flereSider: Boolean,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO exodus_status (tabell, iterator, job_status, antall_rader_hentet, sist_oppdatert)
            VALUES (:tabell, :iterator, :jobStatus, :antallNyeRader, current_timestamp)
            ON CONFLICT (tabell) DO UPDATE SET
                iterator = EXCLUDED.iterator,
                job_status = EXCLUDED.job_status,
                antall_rader_hentet = exodus_status.antall_rader_hentet + EXCLUDED.antall_rader_hentet,
                sist_oppdatert = EXCLUDED.sist_oppdatert
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("tabell", tabellnavn)
                .addValue("iterator", iterator)
                .addValue("jobStatus", if (flereSider) JobStatus.PAGINERER.name else JobStatus.OK.name)
                .addValue("antallNyeRader", antallNyeRader),
        )
    }

    open fun settNyBaseline(tabellnavn: List<String>) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO exodus_status (tabell, iterator, job_status, antall_rader_hentet, sist_oppdatert)
            VALUES (:tabell, NULL, 'NY_BASELINE', 0, current_timestamp)
            ON CONFLICT (tabell) DO UPDATE SET
                iterator = NULL,
                job_status = 'NY_BASELINE',
                antall_rader_hentet = 0,
                sist_oppdatert = current_timestamp
            """.trimIndent(),
            tabellnavn.map { MapSqlParameterSource("tabell", it) }.toTypedArray(),
        )
    }
}
