package no.nav.grunn.og.hjelpestonad.infotrygd.evaluering

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
class EvalueringRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hentEvalueringsraderForVedtak(vedtakIder: Set<Long>): List<Ev10Rad> {
        if (vedtakIder.isEmpty()) return emptyList()
        return jdbcClient
            .sql(
                """
with relevant_sak as (select v.vedtak_id,
                    l.personnr,
                    s10.f_nr as f_nr_ååmmdd,
                    -- ev05_sak er saksblokk fulgt av saksnummer, slik det står i sa_sak_10 (se README, Koblinger mellom tabellene)
                    s10.s05_saksblokk || s10.s10_saksnr as ev05_sak
             from t_vedtak v
                      join t_lopenr_fnr l on l.person_lopenr = v.person_lopenr
                      -- sa_sak_10.f_nr er ÅÅMMDDxxxxx, personnr i t_lopenr_fnr er DDMMÅÅxxxxx
                      join sa_sak_10 s10
                           on s10.f_nr = substr(l.personnr, 5, 2) || substr(l.personnr, 3, 2) || substr(l.personnr, 1, 2) || substr(l.personnr, 7, 5)
                               and s10.s10_kapittelnr = 'GH'
                               and s10.s10_valg = v.kode_rutine
                               and s10.s05_saksblokk = v.saksblokk
                               and s10.s10_saksnr::bigint = v.saksnr
             where v.vedtak_id in (:vedtakIder)),

     -- Siste evaluering for saken/vedtaket. ev05_aar_mnd_seq er 999999 - ÅÅÅÅMM, så laveste verdi er siste måned.
     siste_evaluering_for_vedtak as (select distinct on (relevant_sak.vedtak_id) relevant_sak.vedtak_id,
                                                  relevant_sak.personnr,
                                                  ev05.ev01_personkey,
                                                  ev05.ev05_aar_mnd_seq
               from relevant_sak
                        join ev_valg_05 ev05
                             on ev05.f_nr = relevant_sak.f_nr_ååmmdd
                                 and ev05.ev05_sak = relevant_sak.ev05_sak
                                 and ev05.ev05_kap = 'GH'
                                 and ev05.ev05_tekst_kode = 'EVGH'
               order by relevant_sak.vedtak_id, ev05.ev05_aar_mnd_seq, ev05.id_ev_05 desc),

     -- ev_data_10 kobles bare på personkey og måned. Flere GH-saker i samme måned gjør koblingen tvetydig.
     antall_evalueringer_i_måned as (select s.vedtak_id, count(distinct a.ev05_sak) as antall_evalueringer_i_måneden_på_sak
                      from siste_evaluering_for_vedtak s
                               join ev_valg_05 a
                                    on a.ev01_personkey = s.ev01_personkey
                                        and a.ev05_aar_mnd_seq = s.ev05_aar_mnd_seq
                                        and a.ev05_kap = 'GH'
                                        and a.ev05_tekst_kode = 'EVGH'
                      group by s.vedtak_id)
select s.vedtak_id,
       s.personnr,
       (999999 - s.ev05_aar_mnd_seq::int) as evaluert_måned_ååååmm,
       ev10.id_ev_10,
       ev10.ev10_posisjon,
       ev10.ev10_text,
       n.antall_evalueringer_i_måneden_på_sak
  from siste_evaluering_for_vedtak s
           join antall_evalueringer_i_måned n on n.vedtak_id = s.vedtak_id
           -- Vi henter bare posisjonene i Ev10Posisjon. Andre evalueringer (EVBE, EVZ1 …) kan bruke de samme posisjonene, se Ev10Posisjon i Evaluering.kt og ADR-0010.
           join ev_data_10 ev10
               on ev10.ev01_personkey = s.ev01_personkey
                   and ev10.ev05_aar_mnd_seq = s.ev05_aar_mnd_seq
                   and ev10.ev10_posisjon in (:posisjoner)
order by s.vedtak_id, ev10.ev10_posisjon, ev10.id_ev_10
                """.trimIndent(),
            ).param("vedtakIder", vedtakIder)
            .param("posisjoner", Ev10Posisjon.entries.map { it.kode })
            .query { rs, _ ->
                val månedÅåååmm = rs.getInt("evaluert_måned_ååååmm")
                Ev10Rad(
                    vedtakId = rs.getLong("vedtak_id"),
                    ev10Id = rs.getLong("id_ev_10"),
                    personident = rs.getString("personnr"),
                    månedNotertIInfotrygd = YearMonth.of(månedÅåååmm / 100, månedÅåååmm % 100),
                    antallSakerIMåned = rs.getInt("antall_evalueringer_i_måneden_på_sak"),
                    posisjon = rs.getString("ev10_posisjon"),
                    tekst = rs.getString("ev10_text"),
                )
            }.list()
    }
}
