package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import no.nav.grunn.og.hjelpestonad.infotrygd.util.queryAs
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

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
    private val jdbcClient: JdbcClient,
) {
    open fun finn(tabellnavn: String): ExodusStatus? =
        jdbcClient
            .sql("SELECT * FROM exodus_status WHERE tabell = :tabell")
            .param("tabell", tabellnavn)
            .queryAs<ExodusStatus>()
            .optional()
            .getOrNull()

    open fun oppdaterIterator(
        tabellnavn: String,
        iterator: String?,
        antallNyeRader: Int,
        flereSider: Boolean,
    ) {
        jdbcClient
            .sql(
                """
                INSERT INTO exodus_status (tabell, iterator, job_status, antall_rader_hentet, sist_oppdatert)
                VALUES (:tabell, :iterator, :jobStatus, :antallNyeRader, current_timestamp)
                ON CONFLICT (tabell) DO UPDATE SET
                    iterator = EXCLUDED.iterator,
                    job_status = EXCLUDED.job_status,
                    antall_rader_hentet = exodus_status.antall_rader_hentet + EXCLUDED.antall_rader_hentet,
                    sist_oppdatert = EXCLUDED.sist_oppdatert
                """.trimIndent(),
            ).param("tabell", tabellnavn)
            .param("iterator", iterator)
            .param("jobStatus", if (flereSider) JobStatus.PAGINERER.name else JobStatus.OK.name)
            .param("antallNyeRader", antallNyeRader)
            .update()
    }

    open fun settNyBaseline(tabellnavn: List<String>) {
        tabellnavn.forEach { tabell ->
            jdbcClient
                .sql(
                    """
                    INSERT INTO exodus_status (tabell, iterator, job_status, antall_rader_hentet, sist_oppdatert)
                    VALUES (:tabell, NULL, 'NY_BASELINE', 0, current_timestamp)
                    ON CONFLICT (tabell) DO UPDATE SET
                        iterator = NULL,
                        job_status = 'NY_BASELINE',
                        antall_rader_hentet = 0,
                        sist_oppdatert = current_timestamp
                    """.trimIndent(),
                ).param("tabell", tabell)
                .update()
        }
    }
}
