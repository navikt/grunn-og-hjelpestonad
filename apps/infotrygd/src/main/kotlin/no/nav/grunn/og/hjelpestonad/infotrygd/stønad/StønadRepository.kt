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
     * Finner personer med minst én løpende stønad. Regelen står i [stønadErLøpende].
     */
    fun finnPersonerForMigrering(
        antall: Int,
        dagensDato: LocalDate = LocalDate.now(),
    ): Set<String> =
        jdbcClient
            .sql(
                """
                SELECT DISTINCT l.personnr
                FROM t_lopenr_fnr l
                JOIN t_stonad s ON s.person_lopenr = l.person_lopenr
                WHERE s.kode_rutine IN (:kodeRutiner)
                  AND ${stønadErLøpende("s")}
                ORDER BY l.personnr
                LIMIT :antall
                """.trimIndent(),
            ).param("kodeRutiner", Stønadstype.entries.map { it.kodeRutine })
            .param("nesteMåned", førsteDagINesteMåned(dagensDato))
            .param("antall", antall)
            .queryAs<String>()
            .set()

    /**
     * Finner personer med løpende grunnstønad der stønaden bare har ett gyldig vedtak, og det er et
     * førstegangsvedtak (endringskode F). Vedtaket må ha [sats] i t_gh, og [hjemmelbokstav] i posisjon 160 i siste
     * EVGH-evaluering for saken. Personer med løpende hjelpestønad tas ikke med. Det gjør heller ikke personer med
     * mer enn én GH-sak i evalueringsmåneden, fordi ev_data_10 da ikke kan knyttes til én sak. Virkningstidspunktet
     * (dato_innv_fom på førstegangsvedtaket) må være senest i inneværende måned. Personene sorteres
     * med nyeste virkningstidspunkt (dato_innv_fom på førstegangsvedtaket) først, og på stonad_id ved lik dato.
     *
     * Spørringen starter med GS-stønadene som har ett gyldig vedtak, og sjekker resten per stønad med
     * indeksoppslag. En CTE med alle løpende stønader ga så dårlige anslag i Postgres at spørringen aldri ble
     * ferdig i dev.
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
                WITH grunnstonad_med_ett_vedtak AS MATERIALIZED (
                    SELECT s.stonad_id, min(v.vedtak_id) AS vedtak_id
                    FROM t_stonad s
                    JOIN t_vedtak v ON v.stonad_id = s.stonad_id
                    WHERE s.kode_rutine = :stønadstype
                      AND s.oppdrag_id IS NOT NULL
                      AND ${gyldigVedtak("v", "s")}
                    GROUP BY s.stonad_id
                    HAVING count(*) = 1
                ),
                kandidat AS MATERIALIZED (
                    SELECT l.personnr, s.stonad_id, g.vedtak_id, v.dato_innv_fom
                    FROM grunnstonad_med_ett_vedtak g
                    JOIN t_stonad s ON s.stonad_id = g.stonad_id
                    JOIN t_vedtak v ON v.vedtak_id = g.vedtak_id
                    JOIN t_lopenr_fnr l ON l.person_lopenr = s.person_lopenr
                    WHERE ${stønadErLøpende("s")}
                      -- Virkningstidspunktet må være senest i inneværende måned. Migreringen overtar utbetalingen
                      -- fra inneværende måned, så en stønad som starter senere, har ingenting å ta over ennå.
                      AND v.dato_innv_fom < :nesteMåned
                      AND EXISTS (SELECT 1 FROM t_endring e WHERE e.vedtak_id = g.vedtak_id AND e.kode = 'F')
                      -- Satsen må finnes, og ingen registrering på vedtaket kan ha en annen sats.
                      AND EXISTS (SELECT 1 FROM t_gh gh WHERE gh.vedtak_id = g.vedtak_id)
                      AND NOT EXISTS (
                          SELECT 1 FROM t_gh gh WHERE gh.vedtak_id = g.vedtak_id AND gh.sats_gs IS DISTINCT FROM :sats
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM t_stonad hs
                          WHERE hs.person_lopenr = s.person_lopenr
                            AND hs.kode_rutine = :hjelpestønad
                            AND ${stønadErLøpende("hs")}
                      )
                ),
                -- Siste EVGH-evaluering for saken. ev05_aar_mnd_seq er 999999 - ÅÅÅÅMM, så laveste verdi er siste måned.
                siste_evaluering AS (
                    SELECT DISTINCT ON (k.vedtak_id) k.personnr, k.stonad_id, k.dato_innv_fom, ev05.ev01_personkey, ev05.ev05_aar_mnd_seq
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
                -- En person med flere treff sorteres etter den nyeste stønaden.
                ORDER BY max(se.dato_innv_fom) DESC, max(se.stonad_id) DESC
                LIMIT :antall
                """.trimIndent(),
            ).param("nesteMåned", førsteDagINesteMåned(dagensDato))
            .param("stønadstype", Stønadstype.GRUNNSTØNAD.kodeRutine)
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
         * Vedtaket [vedtak] på stønaden [stønad] er gyldig når det har minst én endringskode som ikke er annullert
         * (AN) eller uavklart (UA), har lengde, og stønaden ikke er opphørt før vedtaket starter.
         */
        fun gyldigVedtak(
            vedtak: String,
            stønad: String,
        ): String =
            """(
                EXISTS (
                    SELECT 1 FROM t_endring endring_$vedtak
                    WHERE endring_$vedtak.vedtak_id = $vedtak.vedtak_id AND endring_$vedtak.kode NOT IN ('AN', 'UA')
                )
                AND ($vedtak.dato_innv_tom IS NULL OR $vedtak.dato_innv_fom < $vedtak.dato_innv_tom)
                AND ($stønad.dato_opphor IS NULL OR $stønad.dato_opphor > $vedtak.dato_innv_fom)
            )"""

        /**
         * Stønaden [stønad] er løpende når den har oppdrag, og siste gyldige vedtak løper forbi starten av neste
         * måned. Sluttdatoen er den tidligste av opphørsdatoen og dato_innv_tom. Er begge tomme, er stønaden
         * løpende. Krever parameteren `nesteMåned`.
         */
        fun stønadErLøpende(stønad: String): String =
            """(
                $stønad.oppdrag_id IS NOT NULL
                AND EXISTS (
                    SELECT 1
                    FROM (
                        SELECT vedtak_$stønad.dato_innv_tom
                        FROM t_vedtak vedtak_$stønad
                        WHERE vedtak_$stønad.stonad_id = $stønad.stonad_id
                          AND ${gyldigVedtak("vedtak_$stønad", stønad)}
                        ORDER BY vedtak_$stønad.vedtak_id DESC
                        LIMIT 1
                    ) siste_vedtak_$stønad
                    WHERE least($stønad.dato_opphor, siste_vedtak_$stønad.dato_innv_tom) IS NULL
                       OR least($stønad.dato_opphor, siste_vedtak_$stønad.dato_innv_tom) > :nesteMåned
                )
            )"""
    }
}
