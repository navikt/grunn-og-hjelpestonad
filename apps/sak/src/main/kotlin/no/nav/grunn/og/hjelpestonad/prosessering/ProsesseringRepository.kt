package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.TaskLogg
import efterlatte.prosessering.TaskLoggType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.sql.ResultSet

@Repository
class ProsesseringRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun hentStatusoversikt(): List<ProsesseringStatusTelling> {
        val antallPerStatus =
            jdbcTemplate
                .query("SELECT status, count(*) AS antall FROM prosessering_task GROUP BY status") { resultSet, _ ->
                    Status.valueOf(resultSet.getString("status")) to resultSet.getLong("antall")
                }.toMap()
        return Status.entries.map { ProsesseringStatusTelling(it, antallPerStatus[it] ?: 0L) }
    }

    fun hentTyper(): List<ProsesseringTypeTelling> =
        jdbcTemplate.query("SELECT type, count(*) AS antall FROM prosessering_task GROUP BY type ORDER BY type") { resultSet, _ ->
            ProsesseringTypeTelling(resultSet.getString("type"), resultSet.getLong("antall"))
        }

    fun hentTask(
        taskId: Long,
        laasForEndring: Boolean = false,
    ): ProsesseringTask? =
        jdbcTemplate
            .query(
                "SELECT * FROM prosessering_task WHERE id = ?" + if (laasForEndring) " FOR UPDATE" else "",
                { resultSet, _ -> tilProsesseringTask(resultSet) },
                taskId,
            ).singleOrNull()

    fun hentTasks(
        status: Status?,
        type: String?,
        side: Int,
        antallPerSide: Int,
    ): ProsesseringTaskSide {
        val filter = lagFilter(status, type)
        val antallTotalt =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM prosessering_task WHERE ${filter.sql}",
                Long::class.java,
                *filter.parametre.toTypedArray(),
            )!!
        val tasks =
            jdbcTemplate.query(
                """
                SELECT * FROM prosessering_task WHERE ${filter.sql}
                ORDER BY sist_endret_tid DESC, id DESC LIMIT ? OFFSET ?
                """.trimIndent(),
                { resultSet, _ -> tilProsesseringTask(resultSet) },
                *(filter.parametre + listOf(antallPerSide, (side.toLong() - 1) * antallPerSide)).toTypedArray(),
            )
        return ProsesseringTaskSide(
            tasks = tasks,
            side = side,
            antallPerSide = antallPerSide,
            antallTotalt = antallTotalt,
            antallSider = (antallTotalt + antallPerSide - 1) / antallPerSide,
        )
    }

    fun hentLogg(taskId: Long): List<TaskLogg> =
        jdbcTemplate.query(
            "SELECT * FROM prosessering_task_hendelse WHERE task_id = ? ORDER BY tidspunkt, id",
            { resultSet, _ ->
                TaskLogg(
                    id = resultSet.getLong("id"),
                    taskId = resultSet.getLong("task_id"),
                    type = TaskLoggType.valueOf(resultSet.getString("type")),
                    melding = resultSet.getString("melding"),
                    endretAv = resultSet.getString("endret_av"),
                    node = resultSet.getString("node"),
                    tidspunkt = resultSet.getTimestamp("tidspunkt").toInstant(),
                )
            },
            taskId,
        )

    fun endreStatus(
        taskId: Long,
        versjon: Long,
        nyStatus: Status,
    ): Boolean =
        jdbcTemplate.update(
            """
            UPDATE prosessering_task
            SET status = ?, versjon = versjon + 1, sist_endret_tid = now(),
                trigger_tid = CASE WHEN ? = 'KLAR' THEN now() ELSE trigger_tid END,
                plukket_tid = NULL,
                stoppaarsak = CASE WHEN ? = 'KLAR' THEN NULL ELSE stoppaarsak END
            WHERE id = ? AND versjon = ?
            """.trimIndent(),
            nyStatus.name,
            nyStatus.name,
            nyStatus.name,
            taskId,
            versjon,
        ) == 1

    fun leggTilHendelse(
        taskId: Long,
        type: TaskLoggType,
        melding: String,
        endretAv: String,
        node: String,
    ) {
        // Bibliotekets leggTil åpner egen transaksjon; her må logg og status dele Spring-transaksjonen.
        jdbcTemplate.update(
            """
            INSERT INTO prosessering_task_hendelse (task_id, type, melding, endret_av, node)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            taskId,
            type.name,
            melding,
            endretAv,
            node,
        )
    }

    private fun tilProsesseringTask(resultSet: ResultSet): ProsesseringTask =
        ProsesseringTask(
            id = resultSet.getLong("id"),
            type = resultSet.getString("type"),
            status = Status.valueOf(resultSet.getString("status")),
            payload = resultSet.getString("payload"),
            metadata = resultSet.getString("metadata")?.let { objectMapper.readValue<Map<String, String>>(it) } ?: emptyMap(),
            antallFeil = resultSet.getInt("antall_feil"),
            stoppaarsak = resultSet.getString("stoppaarsak")?.let(Stoppaarsak::valueOf),
            sisteFeilmelding = resultSet.getString("siste_feilmelding"),
            opprettetTid = resultSet.getTimestamp("opprettet_tid").toInstant(),
            triggerTid = resultSet.getTimestamp("trigger_tid").toInstant(),
            plukketTid = resultSet.getTimestamp("plukket_tid")?.toInstant(),
            sistEndretTid = resultSet.getTimestamp("sist_endret_tid").toInstant(),
            versjon = resultSet.getLong("versjon"),
        )

    private fun lagFilter(
        status: Status?,
        type: String?,
    ): TaskFilter {
        val betingelser = mutableListOf("TRUE")
        val parametre = mutableListOf<Any>()
        if (status != null) {
            betingelser.add("status = ?")
            parametre.add(status.name)
        }
        if (type != null) {
            betingelser.add("type = ?")
            parametre.add(type)
        }
        return TaskFilter(betingelser.joinToString(" AND "), parametre)
    }

    private data class TaskFilter(
        val sql: String,
        val parametre: List<Any>,
    )
}
