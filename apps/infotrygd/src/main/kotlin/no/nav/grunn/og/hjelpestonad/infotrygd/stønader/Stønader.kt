package no.nav.grunn.og.hjelpestonad.infotrygd.stønader

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.InfotrygdEvaluering
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class StonaderRequest(
    val personidenter: Set<String>,
    val stønadstype: Stønadstype,
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}

data class StonaderResponse(
    val stonader: List<StonadResponse>,
)

/** Én rad i t_stonad, med personnr fra t_lopenr_fnr. */
data class StonadResponse(
    val stonadId: Long,
    /** t_lopenr_fnr.personnr */
    val personnr: String,
    val kodeRutine: String,
    val datoStart: LocalDate,
    val kodeOpphor: String?,
    val datoOpphor: LocalDate?,
    val oppdragId: Long?,
    val tidspunktOpphort: LocalDateTime?,
    val tidspunktReg: LocalDateTime?,
    val brukerid: String?,
    val vedtak: List<VedtakResponse> = emptyList(),
)

/** Én rad i t_vedtak. */
data class VedtakResponse(
    val vedtakId: Long,
    val tknr: String,
    val saksblokk: String,
    val saksnr: Long,
    val typeSak: String,
    val kodeResultat: String,
    val datoInnvFom: LocalDate,
    /** Tom betyr at vedtaket er løpende. */
    val datoInnvTom: LocalDate?,
    val datoMottattSak: LocalDate,
    val kodeVedtaksnivaa: String,
    val typeBeregning: String,
    val tidspunktReg: LocalDateTime,
    val brukerid: String,
    /** t_endring.kode for vedtaket, f.eks. F (førstegangsvedtak), O (opphørt) eller AN (annullert). */
    val endringer: List<String> = emptyList(),
    val delytelser: List<DelytelseResponse> = emptyList(),
    val gh: List<GhResponse> = emptyList(),
    /**
     * Avledet, ikke en tabell. Siste EVGH for saken vedtaket hører til (saksblokk og saksnr), se README.
     * Felt vi ikke kan vise én sikker verdi for, står i motstridendeFelt. Null når saken ikke har evaluering.
     * Inneholder helseopplysninger (GDPR art. 9).
     */
    val evaluering: InfotrygdEvaluering? = null,
)

/** Én rad i t_delytelse. Identiteten er typeDelytelse og tidspunktReg innenfor vedtaket. */
data class DelytelseResponse(
    val typeDelytelse: String,
    val tidspunktReg: LocalDateTime,
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: BigDecimal,
    val typeSats: String,
    val typeUtbetaling: String,
    val brukerid: String,
    val linjeId: Long?,
)

/** Én rad i t_gh: trygdetid og sats. Et vedtak kan ha flere registreringer. */
data class GhResponse(
    val tidspunktReg: LocalDateTime,
    val trygdetid: Long,
    val satsGs: Long?,
    val satsHs: Long?,
    val brukerid: String,
)
