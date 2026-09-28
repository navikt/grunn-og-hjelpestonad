package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import no.nav.grunn.og.hjelpestonad.infotrygd.util.datoFraSaTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.util.reverserFnr
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/**
 * TODO: Filtrer på s10_kapittelnr og s10_valg for grunnstønad og hjelpestønad når kodene er kjent,
 *  og map s10_valg til Stønadstype. familie-ef-infotrygd-replika filtrerer på kapittel 'EF' og
 *  valg OG/BT/UT. Inntil videre returneres alle saker for personen, med kapittel og valg.
 */
@Repository
class SakRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun finnesSaker(personidenter: Set<String>): List<Saktreff> =
        jdbcTemplate.query(
            """
            SELECT s.f_nr, s.s10_kapittelnr, s.s10_valg
            FROM sa_sak_10 s
            WHERE s.f_nr IN (:fnr)
            GROUP BY s.f_nr, s.s10_kapittelnr, s.s10_valg
            ORDER BY s.f_nr, s.s10_kapittelnr, s.s10_valg
            """.trimIndent(),
            MapSqlParameterSource("fnr", personidenter.map(String::reverserFnr)),
        ) { rs, _ ->
            Saktreff(
                personident = rs.getString("f_nr").reverserFnr(),
                kapittelnr = rs.getString("s10_kapittelnr"),
                valg = rs.getString("s10_valg"),
            )
        }

    fun finnSaker(personidenter: Set<String>): List<InfotrygdSak> =
        jdbcTemplate.query(
            """
            SELECT s.id_sak, s.s10_saksnr, s.s05_saksblokk, s.s10_reg_dato, s.s10_mottattdato, s.s10_kapittelnr,
                   s.s10_valg, s.s10_undervalg, s.s10_type, s.s10_nivaa, s.s10_resultat, s.s10_vedtaksdato,
                   s.s10_iverksattdato, s.s10_aarsakskode, s.s10_behen_enhet, s.s10_reg_av_enhet, s.tk_nr,
                   s.region, s.f_nr
            FROM sa_sak_10 s
            WHERE s.f_nr IN (:fnr)
            ORDER BY s.id_sak
            """.trimIndent(),
            MapSqlParameterSource("fnr", personidenter.map(String::reverserFnr)),
        ) { rs, _ ->
            InfotrygdSak(
                personident = rs.getString("f_nr").reverserFnr(),
                id = rs.getLong("id_sak"),
                saksnr = rs.getString("s10_saksnr"),
                saksblokk = rs.getString("s05_saksblokk"),
                registrertDato = datoFraSaTabell(rs.getInt("s10_reg_dato")),
                mottattDato = datoFraSaTabell(rs.getInt("s10_mottattdato")),
                kapittelnr = rs.getString("s10_kapittelnr"),
                valg = rs.getString("s10_valg"),
                undervalg = rs.getString("s10_undervalg"),
                type = rs.getString("s10_type"),
                nivå = rs.getString("s10_nivaa"),
                resultat = rs.getString("s10_resultat"),
                vedtaksdato = datoFraSaTabell(rs.getInt("s10_vedtaksdato")),
                iverksattdato = datoFraSaTabell(rs.getInt("s10_iverksattdato")),
                årsakskode = rs.getString("s10_aarsakskode"),
                behandlendeEnhet = rs.getString("s10_behen_enhet"),
                registrertAvEnhet = rs.getString("s10_reg_av_enhet"),
                tkNr = rs.getString("tk_nr"),
                region = rs.getString("region"),
            )
        }
}
