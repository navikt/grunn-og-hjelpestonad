package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusTabell
import no.nav.grunn.og.hjelpestonad.infotrygd.exodus.ExodusUpsertRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.time.YearMonth

/**
 * Lagrer rader i replikatabellene via [ExodusUpsertRepository], som når de kommer fra Exodus. Kolonner
 * som ikke er oppgitt, får en nøytral standardverdi slik at NOT NULL-kravene fra Oracle er oppfylt.
 */
class ReplikaTestdata(
    private val jdbcTemplate: JdbcTemplate,
    private val upsertRepository: ExodusUpsertRepository,
) {
    fun lagre(
        tabell: ExodusTabell,
        vararg verdier: Pair<String, String?>,
    ) {
        val standardverdier =
            jdbcTemplate
                .query(
                    "SELECT column_name, udt_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?",
                    { rs, _ -> rs.getString("column_name") to standardverdi(rs.getString("udt_name")) },
                    tabell.tabellnavn,
                ).toMap()
        upsertRepository.upsert(tabell, listOf(standardverdier + verdier))
    }

    fun tømAlle() {
        upsertRepository.truncate(ExodusTabell.entries)
    }

    fun lagreEvValg05(
        id: String,
        personkey: String,
        årMånedSekvens: String,
        stønadstype: String,
        reversertFnr: String,
        kapittel: String = "GH",
        tekstKode: String = "EVGH",
        sak: String = "",
    ) = lagre(
        ExodusTabell.EV_VALG_05,
        "id_ev_05" to id,
        "ev01_personkey" to personkey,
        "ev05_aar_mnd_seq" to årMånedSekvens,
        "ev05_valg" to stønadstype,
        "f_nr" to reversertFnr,
        "ev05_kap" to kapittel,
        "ev05_tekst_kode" to tekstKode,
        "ev05_sak" to sak,
    )

    fun lagreEvData10(
        id: String,
        personkey: String,
        årMånedSekvens: String,
        tekst: String,
        posisjon: String = "055",
    ) = lagre(
        ExodusTabell.EV_DATA_10,
        "id_ev_10" to id,
        "ev01_personkey" to personkey,
        "ev05_aar_mnd_seq" to årMånedSekvens,
        "ev10_text" to tekst,
        "ev10_posisjon" to posisjon,
    )

    fun lagreTLopenrFnr(
        personLøpenr: String,
        fnr: String,
    ) = lagre(ExodusTabell.T_LOPENR_FNR, "person_lopenr" to personLøpenr, "personnr" to fnr)

    fun lagreTStonad(
        stønadId: String,
        personLøpenr: String,
        stønadstype: String,
    ) = lagre(
        ExodusTabell.T_STONAD,
        "stonad_id" to stønadId,
        "person_lopenr" to personLøpenr,
        "kode_rutine" to stønadstype,
    )

    fun lagreTVedtak(
        vedtakId: String,
        stønadId: String,
        personLøpenr: String,
        stønadstype: String,
        saksblokk: String,
        saksnummer: String,
    ) = lagre(
        ExodusTabell.T_VEDTAK,
        "vedtak_id" to vedtakId,
        "stonad_id" to stønadId,
        "person_lopenr" to personLøpenr,
        "kode_rutine" to stønadstype,
        "saksblokk" to saksblokk,
        "saksnr" to saksnummer,
    )

    fun lagreTDelytelse(vedtakId: String) =
        lagre(
            ExodusTabell.T_DELYTELSE,
            "vedtak_id" to vedtakId,
            "type_delytelse" to "G",
            "tidspunkt_reg" to "2020-01-01T00:00",
        )

    fun lagreTEndring(
        vedtakId: String,
        kode: String,
    ) = lagre(ExodusTabell.T_ENDRING, "vedtak_id" to vedtakId, "kode" to kode)

    fun lagreSaSak10(
        id: String,
        reversertFnr: String,
        saksblokk: String,
        saksnummer: String,
        kapittelnr: String = "GH",
        valg: String = "GS",
    ) = lagre(
        ExodusTabell.SA_SAK_10,
        "id_sak" to id,
        "f_nr" to reversertFnr,
        "s05_saksblokk" to saksblokk,
        "s10_saksnr" to saksnummer,
        "s10_kapittelnr" to kapittelnr,
        "s10_valg" to valg,
    )

    companion object {
        fun ev05Sekvens(årMåned: YearMonth): String = (999999 - årMåned.year * 100 - årMåned.monthValue).toString()
    }

    private fun standardverdi(udtName: String): String? =
        when (udtName) {
            "int8", "numeric" -> "0"
            "date" -> "2020-01-01"
            "timestamp" -> "2020-01-01T00:00"
            else -> ""
        }
}
