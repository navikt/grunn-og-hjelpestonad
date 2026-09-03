package no.nav.gjenlevende.bs.infotrygd.infotrygd.repository

import no.nav.gjenlevende.bs.infotrygd.infotrygd.dto.StønadType
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import kotlin.collections.emptyMap

@Repository
open class InfotrygdRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harStønad(
        personidenter: Set<String>,
        kunAktive: Boolean = false,
        dagensDato: LocalDate = LocalDate.now(),
    ): List<String> {
        val values =
            MapSqlParameterSource()
                .addValue("personidenter", personidenter)
        val filter: String =
            if (kunAktive) {
                values.addValue("dagensDato", dagensDato)
                " AND nvl(S.DATO_OPPHOR,V.DATO_INNV_TOM) > :dagensDato "
            } else {
                ""
            }

        val result =
            jdbcTemplate.query(
                """
                SELECT L.PERSONNR
                  FROM T_BESLUT B
                WHERE L.PERSONNR IN (:personidenter)
                  $filter
                GROUP BY L.personnr
            """,
                values,
            ) { resultSet, _ ->
                resultSet.getString("PERSONNR")
            }
        return result.toList()
    }

    fun test(): List<String> {
        val user: String? =
            jdbcTemplate.queryForObject(
                "SELECT USER FROM DUAL",
                emptyMap<String, Any>(),
                String::class.java,
            )
        val currentSchema: String? =
            jdbcTemplate.queryForObject(
                "SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL",
                emptyMap<String, Any>(),
                String::class.java,
            )

        logger.info("DB USER: $user, CURRENT_SCHEMA: $currentSchema")

        val result =
            jdbcTemplate.query("select TEKST from INFOTRYGD_EBQ.T_GRADSTYPE") { resultSet, _ ->
                resultSet.getString("TEKST")
            }
        return result.toList()
    }

    fun personEksisterer(personident: String): Boolean {
        val params = MapSqlParameterSource().addValue("personident", personident)

        val query =
            """
            SELECT COUNT(*) as ANTALL
            FROM INFOTRYGD_EBQ.T_LOPENR_FNR
            WHERE PERSONNR = :personident
            """.trimIndent()

        val count = jdbcTemplate.queryForObject(query, params, Int::class.java) ?: 0
        return count > 0
    }

    fun hentVedtakPerioderForPerson(personident: String): List<VedtakPeriode> {
        val params = MapSqlParameterSource().addValue("personident", personident)

        val query =
            """
            SELECT
                v.VEDTAK_ID,
                v.STONAD_ID,
                v.KODE_RUTINE,
                v.DATO_INNV_FOM,
                v.DATO_INNV_TOM,
                s.DATO_OPPHOR,
                e.KODE as ENDRING_KODE
            FROM INFOTRYGD_EBQ.T_LOPENR_FNR l
            JOIN INFOTRYGD_EBQ.T_STONAD s ON s.PERSON_LOPENR = l.PERSON_LOPENR
            JOIN INFOTRYGD_EBQ.T_VEDTAK v ON v.STONAD_ID = s.STONAD_ID
            LEFT JOIN INFOTRYGD_EBQ.T_ENDRING e ON e.VEDTAK_ID = v.VEDTAK_ID
            WHERE l.PERSONNR = :personident
              AND s.KODE_RUTINE IN ('GB', 'GU')
              AND s.OPPDRAG_ID IS NOT NULL
              AND (e.KODE IS NULL OR e.KODE NOT IN ('AN', 'UA'))
            ORDER BY v.STONAD_ID DESC, v.VEDTAK_ID DESC, v.DATO_INNV_FOM DESC
            """.trimIndent()

        return jdbcTemplate.query(query, params) { rs, _ ->
            VedtakPeriode(
                vedtakId = rs.getLong("VEDTAK_ID"),
                stønadId = rs.getLong("STONAD_ID"),
                kodeRutine = rs.getString("KODE_RUTINE"),
                datoFom = rs.getDate("DATO_INNV_FOM").toLocalDate(),
                datoTom = rs.getDate("DATO_INNV_TOM")?.toLocalDate(),
                datoOpphør = rs.getDate("DATO_OPPHOR")?.toLocalDate(),
            )
        }
    }

    fun hentRollerForVedtak(vedtakIder: List<Long>): List<RolleData> {
        if (vedtakIder.isEmpty()) return emptyList()

        val params = MapSqlParameterSource().addValue("vedtakIder", vedtakIder)

        val query =
            """
            SELECT
                r.VEDTAK_ID,
                r.PERSON_LOPENR_R,
                r.FOM,
                r.TOM
            FROM INFOTRYGD_EBQ.T_ROLLE r
            WHERE r.VEDTAK_ID IN (:vedtakIder)
            """.trimIndent()

        return jdbcTemplate.query(query, params) { rs, _ ->
            RolleData(
                vedtakId = rs.getLong("VEDTAK_ID"),
                personLøpenummer = rs.getLong("PERSON_LOPENR_R"),
                fom = rs.getDate("FOM").toLocalDate(),
                tom = rs.getDate("TOM")?.toLocalDate(),
            )
        }
    }
}

data class VedtakPeriode(
    val vedtakId: Long,
    val stønadId: Long,
    val kodeRutine: String,
    val datoFom: LocalDate,
    val datoTom: LocalDate?,
    val datoOpphør: LocalDate?,
) {
    val stønadType: StønadType
        get() =
            when (kodeRutine) {
                "GB" -> StønadType.BARNETILSYN
                "GU" -> StønadType.SKOLEPENGER
                else -> throw IllegalArgumentException("Ukjent stønadtype: $kodeRutine")
            }

    fun beregnTomDato(): LocalDate? =
        when {
            datoOpphør != null && datoTom != null && datoOpphør < datoTom -> datoOpphør
            else -> datoTom
        }

    fun erGyldigPeriode(): Boolean {
        val tom = beregnTomDato() ?: return false
        return datoFom < tom && (datoOpphør == null || datoOpphør > datoFom)
    }
}

data class RolleData(
    val vedtakId: Long,
    val personLøpenummer: Long,
    val fom: LocalDate,
    val tom: LocalDate?,
)
