package no.nav.grunn.og.hjelpestonad.infotrygd.tabell

import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.util.queryAs
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class TabellRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hentKolonnerPerTabell(): Map<String, List<String>> =
        jdbcClient
            .sql(
                """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name IN (:tabellnavn)
                ORDER BY table_name, ordinal_position
                """.trimIndent(),
            ).param("tabellnavn", ExodusTabell.entries.map { it.tabellnavn })
            .queryAs<Kolonne>()
            .list()
            .groupBy({ it.tableName }, { it.columnName })

    fun hentAntallRaderPerTabell(): Map<String, Long> =
        ExodusTabell.entries.associate { tabell ->
            val antallRader = jdbcClient.sql("SELECT count(*) FROM ${tabell.tabellnavn}").queryAs<Long>().single()
            tabell.tabellnavn to antallRader
        }

    private data class Kolonne(
        val tableName: String,
        val columnName: String,
    )
}
