package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth

@Repository
class StønadRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun finnVedtakstreff(
        personidenter: Set<String>,
        dagensDato: LocalDate = LocalDate.now(),
    ): List<VedtakstreffResponse> =
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
            VedtakstreffResponse(
                personident = rs.getString("personnr"),
                stønadstype = Stønadstype.fraKodeRutine(rs.getString("kode_rutine")),
                harLøpendeVedtak = rs.getBoolean("har_lopende_vedtak"),
            )
        }

    /**
     * Finner personer der siste vedtak på en stønad løper forbi starten av neste måned. Annullerte (AN) og
     * uavklarte (UA) vedtak, og stønader uten oppdrag, tas ikke med. Sluttdatoen er den tidligste av
     * opphørsdatoen og dato_innv_tom. Er begge tomme, er vedtaket løpende.
     */
    fun finnPersonerForMigrering(
        antall: Int,
        dagensDato: LocalDate = LocalDate.now(),
    ): Set<String> =
        jdbcTemplate
            .query(
                """
                WITH vedtak AS (
                    SELECT l.personnr, s.stonad_id, v.vedtak_id, least(s.dato_opphor, v.dato_innv_tom) AS tom
                    FROM t_lopenr_fnr l
                    JOIN t_stonad s ON s.person_lopenr = l.person_lopenr
                    JOIN t_vedtak v ON v.stonad_id = s.stonad_id
                    JOIN t_endring e ON e.vedtak_id = v.vedtak_id
                    WHERE s.oppdrag_id IS NOT NULL
                      AND s.kode_rutine IN (:kodeRutiner)
                      AND e.kode NOT IN ('AN', 'UA')
                      AND (v.dato_innv_tom IS NULL OR v.dato_innv_fom < v.dato_innv_tom)
                      AND (s.dato_opphor IS NULL OR s.dato_opphor > v.dato_innv_fom)
                ),
                siste_vedtak AS (
                    SELECT stonad_id, max(vedtak_id) AS vedtak_id FROM vedtak GROUP BY stonad_id
                )
                SELECT DISTINCT v.personnr
                FROM siste_vedtak sv
                JOIN vedtak v ON v.vedtak_id = sv.vedtak_id
                WHERE v.tom IS NULL OR v.tom > :nesteMåned
                ORDER BY v.personnr
                LIMIT :antall
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
                    .addValue("nesteMåned", YearMonth.from(dagensDato).plusMonths(1).atDay(1))
                    .addValue("antall", antall),
            ) { rs, _ -> rs.getString("personnr") }
            .toSet()
}
