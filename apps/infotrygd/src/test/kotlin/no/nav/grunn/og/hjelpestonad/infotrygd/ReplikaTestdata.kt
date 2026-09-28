package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Lagrer rader i replikatabellene via [ExodusUpsertRepository], som når de kommer fra Exodus. Kolonner
 * som ikke er oppgitt, får en nøytral standardverdi slik at NOT NULL-kravene fra Oracle er oppfylt.
 */
class ReplikaTestdata(
    private val jdbcTemplate: JdbcTemplate,
    private val upsertRepository: ExodusUpsertRepository,
) {
    fun lagre(
        tabell: ExodusTabell,
        vararg verdier: Pair<String, String?>,
    ) {
        val standardverdier =
            jdbcTemplate
                .query(
                    "SELECT column_name, udt_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?",
                    { rs, _ -> rs.getString("column_name") to standardverdi(rs.getString("udt_name")) },
                    tabell.tabellnavn,
                ).toMap()
        upsertRepository.upsert(tabell, listOf(standardverdier + verdier))
    }

    fun tømAlle() {
        upsertRepository.truncate(ExodusTabell.entries)
    }

    private fun standardverdi(udtName: String): String? =
        when (udtName) {
            "int8", "numeric" -> "0"
            "date" -> "2020-01-01"
            "timestamp" -> "2020-01-01T00:00"
            else -> ""
        }
}
