package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.Ev10Posisjon
import no.nav.grunn.og.hjelpestonad.infotrygd.util.queryAs
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth

@Repository
class StønadRepository(
    private val jdbcClient: JdbcClient,
) {
    fun finnVedtakstreff(
        personidenter: Set<String>,
        dagensDato: LocalDate = LocalDate.now(),
    ): List<VedtakstreffResponse> =
        jdbcClient
            .sql(
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
            ).param("personidenter", personidenter)
            .param("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
            .param("dagensDato", dagensDato)
            .query { rs, _ ->
                VedtakstreffResponse(
                    personident = rs.getString("personnr"),
                    stønadstype = Stønadstype.fraKodeRutine(rs.getString("kode_rutine")),
                    harLøpendeVedtak = rs.getBoolean("har_lopende_vedtak"),
                )
            }.list()

    /**
     * Finner personer der siste vedtak på en stønad løper forbi starten av neste måned. Regelen står i
     * [LØPENDE_STØNADER].
     */
    fun finnPersonerForMigrering(
        antall: Int,
        dagensDato: LocalDate = LocalDate.now(),
    ): Set<String> =
        jdbcClient
            .sql(
                """
                WITH $LØPENDE_STØNADER
                SELECT DISTINCT personnr
                FROM lopende_stonad
                ORDER BY personnr
                LIMIT :antall
                """.trimIndent(),
            ).param("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
            .param("nesteMåned", førsteDagINesteMåned(dagensDato))
            .param("antall", antall)
            .queryAs<String>()
            .set()

    /**
     * Finner personer med løpende grunnstønad der stønaden bare har ett vedtak, og det er et førstegangsvedtak
     * (endringskode F). Vedtaket må ha [sats] i t_gh, og [hjemmelbokstav] i posisjon 160 i siste EVGH-evaluering
     * for saken. Personer med løpende hjelpestønad tas ikke med. Det gjør heller ikke personer med mer enn én
     * GH-sak i evalueringsmåneden, fordi ev_data_10 da ikke kan knyttes til én sak. Personene sorteres på
     * stonad_id.
     */
    fun finnPersonerMedFørstegangsvedtakForMigrering(
        antall: Int,
        sats: Int,
        hjemmelbokstav: String,
        dagensDato: LocalDate = LocalDate.now(),
    ): Set<String> =
        jdbcClient
            .sql(
                """
                WITH $LØPENDE_STØNADER,
                antall_gyldige_vedtak_per_stonad AS (
                    SELECT stonad_id, count(*) AS antall_vedtak FROM vedtak GROUP BY stonad_id
                ),
                kandidat AS MATERIALIZED (
                    SELECT ls.personnr, ls.stonad_id, ls.vedtak_id
                    FROM lopende_stonad ls
                    JOIN antall_gyldige_vedtak_per_stonad a ON a.stonad_id = ls.stonad_id AND a.antall_vedtak = 1
                    WHERE ls.kode_rutine = :grunnstønad
                      AND EXISTS (SELECT 1 FROM t_endring e WHERE e.vedtak_id = ls.vedtak_id AND e.kode = 'F')
                      -- Satsen må finnes, og ingen registrering på vedtaket kan ha en annen sats.
                      AND EXISTS (SELECT 1 FROM t_gh g WHERE g.vedtak_id = ls.vedtak_id)
                      AND NOT EXISTS (
                          SELECT 1 FROM t_gh g WHERE g.vedtak_id = ls.vedtak_id AND g.sats_gs IS DISTINCT FROM :sats
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM lopende_stonad hs
                          WHERE hs.personnr = ls.personnr AND hs.kode_rutine = :hjelpestønad
                      )
                ),
                -- Siste EVGH-evaluering for saken. ev05_aar_mnd_seq er 999999 - ÅÅÅÅMM, så laveste verdi er siste måned.
                siste_evaluering AS (
                    SELECT DISTINCT ON (k.vedtak_id) k.personnr, k.stonad_id, ev05.ev01_personkey, ev05.ev05_aar_mnd_seq
                    FROM kandidat k
                    JOIN t_vedtak v ON v.vedtak_id = k.vedtak_id
                    -- sa_sak_10.f_nr er ÅÅMMDDxxxxx, personnr i t_lopenr_fnr er DDMMÅÅxxxxx
                    JOIN sa_sak_10 s10
                        ON s10.f_nr = substr(k.personnr, 5, 2) || substr(k.personnr, 3, 2) || substr(k.personnr, 1, 2) || substr(k.personnr, 7, 5)
                       AND s10.s10_kapittelnr = 'GH'
                       AND s10.s10_valg = v.kode_rutine
                       AND s10.s05_saksblokk = v.saksblokk
                       AND s10.s10_saksnr::bigint = v.saksnr
                    JOIN ev_valg_05 ev05
                        ON ev05.f_nr = s10.f_nr
                       AND ev05.ev05_sak = s10.s05_saksblokk || s10.s10_saksnr
                       AND ev05.ev05_kap = 'GH'
                       AND ev05.ev05_tekst_kode = 'EVGH'
                    ORDER BY k.vedtak_id, ev05.ev05_aar_mnd_seq, ev05.id_ev_05 DESC
                )
                SELECT se.personnr
                FROM siste_evaluering se
                -- ev_data_10 kobles bare på personkey og måned. Med flere GH-saker i måneden kan vi ikke vite
                -- hvilken sak hjemmelbokstaven hører til. For hjelpestønad står det 1 eller 2 i posisjon 160.
                WHERE (
                        SELECT count(DISTINCT a.ev05_sak)
                        FROM ev_valg_05 a
                        WHERE a.ev01_personkey = se.ev01_personkey
                          AND a.ev05_aar_mnd_seq = se.ev05_aar_mnd_seq
                          AND a.ev05_kap = 'GH'
                          AND a.ev05_tekst_kode = 'EVGH'
                    ) = 1
                  AND EXISTS (
                      SELECT 1 FROM ev_data_10 d
                      WHERE d.ev01_personkey = se.ev01_personkey
                        AND d.ev05_aar_mnd_seq = se.ev05_aar_mnd_seq
                        AND d.ev10_posisjon = :posisjonHjemmelbokstav
                        AND trim(d.ev10_text) = :hjemmelbokstav
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM ev_data_10 d
                      WHERE d.ev01_personkey = se.ev01_personkey
                        AND d.ev05_aar_mnd_seq = se.ev05_aar_mnd_seq
                        AND d.ev10_posisjon = :posisjonHjemmelbokstav
                        AND trim(d.ev10_text) IS DISTINCT FROM :hjemmelbokstav
                  )
                GROUP BY se.personnr
                ORDER BY min(se.stonad_id)
                LIMIT :antall
                """.trimIndent(),
            ).param("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
            .param("nesteMåned", førsteDagINesteMåned(dagensDato))
            .param("grunnstønad", Stønadstype.GRUNNSTØNAD.kodeRutine)
            .param("hjelpestønad", Stønadstype.HJELPESTØNAD.kodeRutine)
            .param("sats", sats)
            .param("posisjonHjemmelbokstav", Ev10Posisjon.HJEMMELBOKSTAV.kode)
            .param("hjemmelbokstav", hjemmelbokstav)
            .param("antall", antall)
            .queryAs<String>()
            .set()

    private fun førsteDagINesteMåned(dagensDato: LocalDate): LocalDate = YearMonth.from(dagensDato).plusMonths(1).atDay(1)

    private companion object {
        /**
         * CTE-er for løpende stønader, delt mellom spørringene for migrering. Krever parameterne `kodeRutiner` og
         * `nesteMåned`.
         *
         * - `vedtak`: gyldige vedtak. Annullerte (AN) og uavklarte (UA) vedtak, vedtak uten lengde og stønader uten
         *   oppdrag tas ikke med.
         * - `lopende_stonad`: siste gyldige vedtak per stønad, når det løper forbi starten av neste måned.
         *   Sluttdatoen er den tidligste av opphørsdatoen og dato_innv_tom. Er begge tomme, er vedtaket løpende.
         */
        const val LØPENDE_STØNADER = """
                vedtak AS (
                    SELECT DISTINCT l.personnr, s.stonad_id, s.kode_rutine, v.vedtak_id,
                           least(s.dato_opphor, v.dato_innv_tom) AS tom
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
                ),
                lopende_stonad AS (
                    SELECT v.personnr, v.stonad_id, v.kode_rutine, v.vedtak_id
                    FROM siste_vedtak sv
                    JOIN vedtak v ON v.vedtak_id = sv.vedtak_id
                    WHERE v.tom IS NULL OR v.tom > :nesteMåned
                )"""
    }
}
