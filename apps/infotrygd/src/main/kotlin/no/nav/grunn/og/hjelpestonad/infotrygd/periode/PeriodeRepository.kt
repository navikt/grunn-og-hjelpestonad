package no.nav.grunn.og.hjelpestonad.infotrygd.periode

import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tilsvarer PeriodeRepository i familie-ef-infotrygd-replika, men beløp og periode hentes fra t_delytelse og
 * trygdetid og sats fra t_gh, i stedet for fra t_ef.
 *
 * Vedtak der dato_innv_tom er før eller lik dato_innv_fom, filtreres bort. Vedtak uten dato_innv_tom er løpende
 * og tas med.
 */
@Repository
class PeriodeRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hentPerioder(
        personidenter: Set<String>,
        stønadstyper: Set<Stønadstype>,
    ): List<PeriodeResponse> =
        jdbcClient
            .sql(
                """
                SELECT l.personnr,
                       v.kode_rutine,
                       e.kode,
                       v.brukerid,
                       s.stonad_id,
                       v.vedtak_id,
                       v.tidspunkt_reg,
                       v.type_sak,
                       v.kode_resultat,
                       v.dato_innv_fom,
                       v.dato_innv_tom,
                       s.dato_start,
                       s.dato_opphor,
                       s.oppdrag_id,
                       d.type_delytelse,
                       d.type_sats,
                       d.type_utbetaling,
                       d.fom,
                       d.tom,
                       d.belop
                FROM t_lopenr_fnr l
                JOIN t_stonad s ON s.person_lopenr = l.person_lopenr
                JOIN t_vedtak v ON v.stonad_id = s.stonad_id
                JOIN t_endring e ON e.vedtak_id = v.vedtak_id
                JOIN t_delytelse d ON d.vedtak_id = v.vedtak_id
                WHERE l.personnr IN (:personidenter)
                  AND v.kode_rutine IN (:kodeRutiner)
                  AND (v.dato_innv_tom IS NULL OR v.dato_innv_fom < v.dato_innv_tom)
                ORDER BY s.stonad_id, v.vedtak_id, d.fom DESC
                """.trimIndent(),
            ).param("personidenter", personidenter)
            .param("kodeRutiner", stønadstyper.map { it.kodeRutine })
            .query { rs, _ ->
                PeriodeResponse(
                    personident = rs.getString("personnr"),
                    stønadstype = Stønadstype.fraKodeRutine(rs.getString("kode_rutine")),
                    sakstype = rs.getString("type_sak").trim(),
                    kode = rs.getString("kode").trim(),
                    brukerId = rs.getString("brukerid"),
                    stønadId = rs.getLong("stonad_id"),
                    vedtakId = rs.getLong("vedtak_id"),
                    vedtakstidspunkt = rs.getObject("tidspunkt_reg", LocalDateTime::class.java),
                    vedtakKodeResultat = rs.getString("kode_resultat").trim(),
                    startDato = rs.getObject("dato_start", LocalDate::class.java),
                    innvilgetFom = rs.getObject("dato_innv_fom", LocalDate::class.java),
                    innvilgetTom = rs.getObject("dato_innv_tom", LocalDate::class.java),
                    opphørsdato = rs.getObject("dato_opphor", LocalDate::class.java),
                    oppdragId = rs.getNullableLong("oppdrag_id"),
                    typeDelytelse = rs.getString("type_delytelse").trim(),
                    typeSats = rs.getString("type_sats").trim(),
                    typeUtbetaling = rs.getString("type_utbetaling").trim(),
                    stønadFom = rs.getObject("fom", LocalDate::class.java),
                    stønadTom = rs.getObject("tom", LocalDate::class.java),
                    beløp = rs.getBigDecimal("belop"),
                )
            }.list()

    // TODO: Tror egentlig ikke denne måten å presentere data gir mening.
    //  Å hente periodene og så vedtakene periode tror jeg ikke er det lettese måten å se på dataene.
    //  Holder det sånn enn så lenge fortdi det er ganske likt som i familie-ef-infotrygd-replika, men bør vurderes å endres.
    fun hentTrygdetidOgSats(vedtakIder: Set<Long>): Map<Long, List<TrygdetidOgSatsResponse>> {
        if (vedtakIder.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                SELECT g.vedtak_id, g.tidspunkt_reg, g.trygdetid, g.sats_gs, g.sats_hs, g.brukerid
                FROM t_gh g
                WHERE g.vedtak_id IN (:vedtakIder)
                ORDER BY g.vedtak_id, g.tidspunkt_reg DESC
                """.trimIndent(),
            ).param("vedtakIder", vedtakIder)
            .query { rs, _ ->
                TrygdetidOgSatsForVedtak(
                    vedtakId = rs.getLong("vedtak_id"),
                    trygdetidOgSats =
                        TrygdetidOgSatsResponse(
                            tidspunktRegistrert = rs.getObject("tidspunkt_reg", LocalDateTime::class.java),
                            trygdetid = rs.getLong("trygdetid"),
                            satsGs = rs.getNullableLong("sats_gs"),
                            satsHs = rs.getNullableLong("sats_hs"),
                            brukerId = rs.getString("brukerid"),
                        ),
                )
            }.list()
            .groupBy({ it.vedtakId }, { it.trygdetidOgSats })
    }

    private data class TrygdetidOgSatsForVedtak(
        val vedtakId: Long,
        val trygdetidOgSats: TrygdetidOgSatsResponse,
    )

    private fun ResultSet.getNullableLong(kolonne: String): Long? = getObject(kolonne, Long::class.javaObjectType)
}
