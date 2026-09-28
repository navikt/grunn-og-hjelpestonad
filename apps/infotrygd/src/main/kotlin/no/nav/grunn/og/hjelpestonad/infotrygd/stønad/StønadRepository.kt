package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class StønadRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun finnVedtakstreff(
        personidenter: Set<String>,
        dagensDato: LocalDate = LocalDate.now(),
    ): List<Vedtakstreff> =
        jdbcTemplate.query(
            """
            SELECT l.personnr,
                   s.kode_rutine,
                   bool_or(coalesce(s.dato_opphor, v.dato_innv_tom) IS NULL
                       OR coalesce(s.dato_opphor, v.dato_innv_tom) > :dagensDato) AS har_lopende_vedtak
            FROM t_lopenr_fnr l
            JOIN t_stonad s ON s.person_lopenr = l.person_lopenr
            JOIN t_vedtak v ON v.stonad_id = s.stonad_id
            WHERE l.personnr IN (:personidenter)
              AND s.kode_rutine IN (:kodeRutiner)
            GROUP BY l.personnr, s.kode_rutine
            ORDER BY l.personnr, s.kode_rutine
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("personidenter", personidenter)
                .addValue("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
                .addValue("dagensDato", dagensDato),
        ) { rs, _ ->
            Vedtakstreff(
                personident = rs.getString("personnr"),
                stønadstype = Stønadstype.fraKodeRutine(rs.getString("kode_rutine")),
                harLøpendeVedtak = rs.getBoolean("har_lopende_vedtak"),
            )
        }
}
