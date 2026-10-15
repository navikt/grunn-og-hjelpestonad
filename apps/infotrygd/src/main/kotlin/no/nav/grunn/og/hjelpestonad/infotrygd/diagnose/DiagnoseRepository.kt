package no.nav.grunn.og.hjelpestonad.infotrygd.diagnose

import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.YearMonth

@Repository
class DiagnoseRepository(
    private val jdbcClient: JdbcClient,
) {
    fun hentSisteDiagnoser(
        personidenter: Set<String>,
        stønadstyper: Set<Stønadstype>,
    ): List<InfotrygdDiagnose> =
        jdbcClient
            .sql(
                """
                with relevante_personer as (select fnr_ddmmåå,
                                                   substr(fnr_ddmmåå, 5, 2) || substr(fnr_ddmmåå, 3, 2) || substr(fnr_ddmmåå, 1, 2) ||
                                                   substr(fnr_ddmmåå, 7, 5) as fnr_ååmmdd
                                            from unnest(array[:fnr]::text[]) as v(fnr_ddmmåå))
                select distinct rp.fnr_ddmmåå,
                       ev05.ev05_valg,
                       (999999 - ev05.ev05_aar_mnd_seq::int) as diagnose_ååååmm,
                       ev05.endret_i_kilde,
                       substr(ev10.ev10_text, 1, 4) as diagnosekode
                from relevante_personer rp
                         join ev_valg_05 ev05
                              on ev05.f_nr = rp.fnr_ååmmdd
                         join ev_data_10 ev10
                              on ev10.ev01_personkey = ev05.ev01_personkey
                                  and ev10.ev05_aar_mnd_seq = ev05.ev05_aar_mnd_seq
                where ev05.ev05_tekst_kode = 'EVGH'
                  and ev05.ev05_kap = 'GH'
                  and ev05.ev05_valg in (:kodeRutiner)
                  -- ev05_aar_mnd_seq er 999999 - ÅÅÅÅMM, så min gir siste måned. Siste måned per person og stønadstype.
                  and ev05.ev05_aar_mnd_seq = (select min(ev05_kopi.ev05_aar_mnd_seq)
                                               from ev_valg_05 ev05_kopi
                                               where ev05_kopi.ev01_personkey = ev05.ev01_personkey
                                                 and ev05_kopi.ev05_tekst_kode = 'EVGH'
                                                 and ev05_kopi.ev05_kap = 'GH'
                                                 and ev05_kopi.ev05_valg = ev05.ev05_valg)
                  -- Personen har minst ett gyldig vedtak på stønaden diagnosen gjelder
                  and exists (select 1
                              from t_lopenr_fnr fnr
                                       join t_stonad ston
                                            on ston.person_lopenr = fnr.person_lopenr
                                                and ston.kode_rutine = ev05.ev05_valg
                                       join t_vedtak vedt
                                            on vedt.person_lopenr = ston.person_lopenr
                                                and vedt.stonad_id = ston.stonad_id
                              where fnr.personnr = rp.fnr_ddmmåå
                                -- GH/HS-sak som vedtaket hører til
                                and exists (select 1
                                            from sa_sak_10 s10
                                            where s10.f_nr = ev05.f_nr
                                              and s10.s10_kapittelnr = 'GH'
                                              and s10.s05_saksblokk = vedt.saksblokk
                                              and s10.s10_saksnr::bigint = vedt.saksnr)
                                -- Vedtaket har minst én delytelse
                                and exists (select 1
                                            from t_delytelse dely
                                            where dely.vedtak_id = vedt.vedtak_id)
                                -- Ikke annullert eller ugyldig
                                and not exists (select 1
                                                from t_endring endr
                                                where endr.vedtak_id = vedt.vedtak_id
                                                  and endr.kode in ('UA', 'AN')))
                order by rp.fnr_ddmmåå, ev05.ev05_valg, diagnose_ååååmm, diagnosekode
                """.trimIndent(),
            ).param("fnr", personidenter.toList())
            .param("kodeRutiner", stønadstyper.map { it.kodeRutine })
            .query { rs, _ ->
                val diagnoseÅåååmm = rs.getInt("diagnose_ååååmm")
                InfotrygdDiagnose(
                    personident = rs.getString("fnr_ddmmåå"),
                    stønadstype = Stønadstype.fraKodeRutine(rs.getString("ev05_valg")),
                    månedDiagnoseNotertIInfotrygd = YearMonth.of(diagnoseÅåååmm / 100, diagnoseÅåååmm % 100),
                    diagnosekode = rs.getString("diagnosekode"),
                    datoEndretIKilde = rs.getObject("endret_i_kilde", LocalDateTime::class.java),
                )
            }.list()
}
