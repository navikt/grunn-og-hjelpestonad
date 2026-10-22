package no.nav.grunn.og.hjelpestonad.infotrygd.stønader

import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Én spørring per tabell. Barnetabellene hentes med IN på nøklene fra forelderen, så antall spørringer er fast
 * uansett hvor mange vedtak personen har. Ingen spørring joiner to barnetabeller, fordi det ganger opp radene.
 *
 * Kall metodene i samme transaksjon, se [StønaderService].
 */
@Repository
class StønaderRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hentStønader(
        personidenter: Set<String>,
        stønadstype: Stønadstype,
    ): List<StonadResponse> {
        if (personidenter.isEmpty()) return emptyList()
        return jdbcClient
            .sql(
                """
                SELECT s.stonad_id,
                       l.personnr,
                       s.kode_rutine,
                       s.dato_start,
                       s.kode_opphor,
                       s.dato_opphor,
                       s.oppdrag_id,
                       s.tidspunkt_opphort,
                       s.tidspunkt_reg,
                       s.brukerid
                FROM t_stonad s
                JOIN t_lopenr_fnr l ON l.person_lopenr = s.person_lopenr
                WHERE l.personnr IN (:personidenter)
                  AND s.kode_rutine = :kodeRutine
                ORDER BY s.stonad_id
                """.trimIndent(),
            ).param("personidenter", personidenter)
            .param("kodeRutine", stønadstype.kodeRutine)
            .query { rs, _ ->
                StonadResponse(
                    stonadId = rs.getLong("stonad_id"),
                    personnr = rs.tekst("personnr"),
                    kodeRutine = rs.tekst("kode_rutine"),
                    datoStart = rs.dato("dato_start"),
                    kodeOpphor = rs.tekstEllerNull("kode_opphor"),
                    datoOpphor = rs.datoEllerNull("dato_opphor"),
                    oppdragId = rs.longEllerNull("oppdrag_id"),
                    tidspunktOpphort = rs.tidspunktEllerNull("tidspunkt_opphort"),
                    tidspunktReg = rs.tidspunktEllerNull("tidspunkt_reg"),
                    brukerid = rs.tekstEllerNull("brukerid"),
                )
            }.list()
    }

    /**
     * Vedtakene per stonad_id. Listene fra barnetabellene er tomme og fylles av [StønaderService].
     *
     * Vedtaket må også høre til en av [personidenter], ikke bare til en stønad vi har hentet. Tilgangen er bare
     * sjekket for personidentene i forespørselen. Replikaen har ingen fremmednøkler, og t_vedtak.person_lopenr kan
     * avvike fra t_stonad.person_lopenr eller mangle i t_lopenr_fnr (målt i dev 2026-10-22: fire vedtak). Uten
     * filteret kunne et slikt vedtak vises under feil person. Barnetabellene og evalueringen arver eierskapet fra vedtaket.
     */
    fun hentVedtak(
        stønadIder: Set<Long>,
        personidenter: Set<String>,
    ): Map<Long, List<VedtakResponse>> {
        if (stønadIder.isEmpty() || personidenter.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                SELECT v.stonad_id,
                       v.vedtak_id,
                       v.tknr,
                       v.saksblokk,
                       v.saksnr,
                       v.type_sak,
                       v.kode_resultat,
                       v.dato_innv_fom,
                       v.dato_innv_tom,
                       v.dato_mottatt_sak,
                       v.kode_vedtaksnivaa,
                       v.type_beregning,
                       v.tidspunkt_reg,
                       v.brukerid
                FROM t_vedtak v
                WHERE v.stonad_id IN (:stonadIder)
                  AND v.person_lopenr IN (
                      SELECT l.person_lopenr
                      FROM t_lopenr_fnr l
                      WHERE l.personnr IN (:personidenter)
                  )
                ORDER BY v.stonad_id, v.vedtak_id
                """.trimIndent(),
            ).param("stonadIder", stønadIder)
            .param("personidenter", personidenter)
            .query { rs, _ ->
                rs.getLong("stonad_id") to
                    VedtakResponse(
                        vedtakId = rs.getLong("vedtak_id"),
                        tknr = rs.tekst("tknr"),
                        saksblokk = rs.tekst("saksblokk"),
                        saksnr = rs.getLong("saksnr"),
                        typeSak = rs.tekst("type_sak"),
                        kodeResultat = rs.tekst("kode_resultat"),
                        datoInnvFom = rs.dato("dato_innv_fom"),
                        datoInnvTom = rs.datoEllerNull("dato_innv_tom"),
                        datoMottattSak = rs.dato("dato_mottatt_sak"),
                        kodeVedtaksnivaa = rs.tekst("kode_vedtaksnivaa"),
                        typeBeregning = rs.tekst("type_beregning"),
                        tidspunktReg = rs.tidspunkt("tidspunkt_reg"),
                        brukerid = rs.tekst("brukerid"),
                    )
            }.list()
            .groupBy({ it.first }, { it.second })
    }

    /** t_endring.kode per vedtak_id. */
    fun hentEndringer(vedtakIder: Set<Long>): Map<Long, List<String>> {
        if (vedtakIder.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                SELECT e.vedtak_id, e.kode
                FROM t_endring e
                WHERE e.vedtak_id IN (:vedtakIder)
                ORDER BY e.vedtak_id, e.kode
                """.trimIndent(),
            ).param("vedtakIder", vedtakIder)
            .query { rs, _ -> rs.getLong("vedtak_id") to rs.tekst("kode") }
            .list()
            .groupBy({ it.first }, { it.second })
    }

    fun hentDelytelser(vedtakIder: Set<Long>): Map<Long, List<DelytelseResponse>> {
        if (vedtakIder.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                SELECT d.vedtak_id,
                       d.type_delytelse,
                       d.tidspunkt_reg,
                       d.fom,
                       d.tom,
                       d.belop,
                       d.type_sats,
                       d.type_utbetaling,
                       d.brukerid,
                       d.linje_id
                FROM t_delytelse d
                WHERE d.vedtak_id IN (:vedtakIder)
                ORDER BY d.vedtak_id, d.type_delytelse, d.tidspunkt_reg
                """.trimIndent(),
            ).param("vedtakIder", vedtakIder)
            .query { rs, _ ->
                rs.getLong("vedtak_id") to
                    DelytelseResponse(
                        typeDelytelse = rs.tekst("type_delytelse"),
                        tidspunktReg = rs.tidspunkt("tidspunkt_reg"),
                        fom = rs.dato("fom"),
                        tom = rs.datoEllerNull("tom"),
                        belop = rs.getBigDecimal("belop"),
                        typeSats = rs.tekst("type_sats"),
                        typeUtbetaling = rs.tekst("type_utbetaling"),
                        brukerid = rs.tekst("brukerid"),
                        linjeId = rs.longEllerNull("linje_id"),
                    )
            }.list()
            .groupBy({ it.first }, { it.second })
    }

    fun hentGh(vedtakIder: Set<Long>): Map<Long, List<GhResponse>> {
        if (vedtakIder.isEmpty()) return emptyMap()
        return jdbcClient
            .sql(
                """
                SELECT g.vedtak_id,
                       g.tidspunkt_reg,
                       g.trygdetid,
                       g.sats_gs,
                       g.sats_hs,
                       g.brukerid
                FROM t_gh g
                WHERE g.vedtak_id IN (:vedtakIder)
                ORDER BY g.vedtak_id, g.tidspunkt_reg
                """.trimIndent(),
            ).param("vedtakIder", vedtakIder)
            .query { rs, _ ->
                rs.getLong("vedtak_id") to
                    GhResponse(
                        tidspunktReg = rs.tidspunkt("tidspunkt_reg"),
                        trygdetid = rs.getLong("trygdetid"),
                        satsGs = rs.longEllerNull("sats_gs"),
                        satsHs = rs.longEllerNull("sats_hs"),
                        brukerid = rs.tekst("brukerid"),
                    )
            }.list()
            .groupBy({ it.first }, { it.second })
    }

    private fun ResultSet.tekst(kolonne: String): String = getString(kolonne).trim()

    private fun ResultSet.tekstEllerNull(kolonne: String): String? = getString(kolonne)?.trim()

    private fun ResultSet.dato(kolonne: String): LocalDate = getObject(kolonne, LocalDate::class.java)

    private fun ResultSet.datoEllerNull(kolonne: String): LocalDate? = getObject(kolonne, LocalDate::class.java)

    private fun ResultSet.tidspunkt(kolonne: String): LocalDateTime = getObject(kolonne, LocalDateTime::class.java)

    private fun ResultSet.tidspunktEllerNull(kolonne: String): LocalDateTime? = getObject(kolonne, LocalDateTime::class.java)

    private fun ResultSet.longEllerNull(kolonne: String): Long? = getObject(kolonne, Long::class.javaObjectType)
}
