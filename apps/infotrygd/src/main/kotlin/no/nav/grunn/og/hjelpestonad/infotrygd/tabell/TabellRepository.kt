package no.nav.grunn.og.hjelpestonad.infotrygd.tabell

import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class TabellRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun hentKolonnerPerTabell(): Map<String, List<String>> {
        val kolonner =
            jdbcTemplate.query(
                """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name IN (:tabellnavn)
                ORDER BY table_name, ordinal_position
                """.trimIndent(),
                MapSqlParameterSource("tabellnavn", ExodusTabell.entries.map { it.tabellnavn }),
            ) { rs, _ -> rs.getString("table_name") to rs.getString("column_name") }
        return kolonner.groupBy({ it.first }, { it.second })
    }

    /** Tabellnavnene kommer fra [ExodusTabell] og kan derfor settes rett inn i SQL. */
    fun hentAntallRaderPerTabell(): Map<String, Long> =
        ExodusTabell.entries.associate { tabell ->
            tabell.tabellnavn to
                (jdbcTemplate.queryForObject("SELECT count(*) FROM ${tabell.tabellnavn}", emptyMap<String, Any>(), Long::class.java) ?: 0)
        }
}
