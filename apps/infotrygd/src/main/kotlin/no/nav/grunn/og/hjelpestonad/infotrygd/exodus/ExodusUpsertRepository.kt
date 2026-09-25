package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Generisk upsert av rader fra Exodus, basert på primærnøkkelen definert i [ExodusTabell]. Exodus
 * serialiserer alle verdier som tekst, og Postgres caster ikke implisitt fra tekst til
 * numeric/date/timestamp. Hver verdi castes derfor eksplisitt til kolonnens faktiske type, slått
 * opp via information_schema og cachet per tabell.
 */
@Repository
open class ExodusUpsertRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val kolonnetyperPerTabell = ConcurrentHashMap<String, Map<String, String>>()

    open fun upsert(
        tabell: ExodusTabell,
        rader: List<Map<String, String?>>,
    ) {
        if (rader.isEmpty()) return

        val kolonner =
            rader
                .first()
                .keys
                .map { it.lowercase() }
                .distinct()
        val primærnøkkel = tabell.primærnøkkel
        val settKolonner = kolonner - primærnøkkel.toSet()
        val kolonnetyper = hentKolonnetyper(tabell)

        // Kolonnenavnene kommer fra Exodus-responsen og settes rett inn i SQL, så de må finnes i tabellen.
        val ukjenteKolonner = kolonner - kolonnetyper.keys
        check(ukjenteKolonner.isEmpty()) {
            "Tabell ${tabell.tabellnavn} i Postgres mangler kolonnene $ukjenteKolonner som Exodus sender"
        }

        val sql =
            buildString {
                append("INSERT INTO ${tabell.tabellnavn} (${kolonner.joinToString(", ")}) ")
                append("VALUES (${kolonner.joinToString(", ") { verdiuttrykk(it, kolonnetyper.getValue(it)) }}) ")
                append("ON CONFLICT (${primærnøkkel.joinToString(", ")}) ")
                if (settKolonner.isEmpty()) {
                    append("DO NOTHING")
                } else {
                    append("DO UPDATE SET ${settKolonner.joinToString(", ") { "$it = EXCLUDED.$it" }}")
                }
            }

        val parametre =
            rader
                .map { rad ->
                    val saneRad =
                        rad
                            .mapKeys { (kolonnenavn, _) -> kolonnenavn.lowercase() }
                            .mapValues { (_, verdi) -> vask(verdi) }
                    MapSqlParameterSource(saneRad)
                }.toTypedArray()

        jdbcTemplate.batchUpdate(sql, parametre)
    }

    /** Ved ny baseline er alle replikerte data ugyldige, så alle tabellene tømmes i én operasjon. */
    open fun truncate(tabeller: List<ExodusTabell>) {
        if (tabeller.isEmpty()) return
        jdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE ${tabeller.joinToString(", ") { it.tabellnavn }}")
    }

    /** Postgres sitt interne typenavn (udt_name) per kolonne, f.eks. numeric/date/timestamp/bpchar/text. */
    private fun hentKolonnetyper(tabell: ExodusTabell): Map<String, String> =
        kolonnetyperPerTabell.getOrPut(tabell.tabellnavn) {
            jdbcTemplate.jdbcTemplate
                .query(
                    """
                    SELECT column_name, udt_name
                    FROM information_schema.columns
                    WHERE table_schema = current_schema() AND table_name = ?
                    """.trimIndent(),
                    { rs, _ -> rs.getString("column_name") to rs.getString("udt_name") },
                    tabell.tabellnavn,
                ).toMap()
                .also {
                    check(it.isNotEmpty()) { "Tabell ${tabell.tabellnavn} finnes ikke i Postgres. Mangler Flyway-migrering?" }
                }
        }

    /** Binærdata kommer som hex fra Exodus. */
    private fun verdiuttrykk(
        kolonne: String,
        udtName: String,
    ): String =
        when (udtName) {
            "bytea" -> "decode(:$kolonne, 'hex')"
            else -> "CAST(:$kolonne AS $udtName)"
        }

    /**
     * Postgres tillater aldri NUL-tegn i tekst, noe enkelte Oracle CHAR-kolonner inneholder. Oracle
     * CHAR er også fylt ut med mellomrom til full lengde, som fjernes slik at 'GB  ' blir 'GB'.
     */
    private fun vask(verdi: String?): String? = verdi?.replace("\u0000", "")?.trimEnd(' ')
}
