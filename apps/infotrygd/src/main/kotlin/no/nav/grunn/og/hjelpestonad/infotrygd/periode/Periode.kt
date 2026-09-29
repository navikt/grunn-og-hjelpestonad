package no.nav.grunn.og.hjelpestonad.infotrygd.periode

import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param stønadstyper tom betyr alle stønadstyper
 */
data class PeriodeRequest(
    val personidenter: Set<String>,
    val stønadstyper: Set<Stønadstype> = emptySet(),
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}

data class PerioderResponse(
    val grunnstønad: List<PeriodeResponse>,
    val hjelpestønad: List<PeriodeResponse>,
)

/**
 * Én periode per rad i t_delytelse, med informasjon om vedtaket og stønaden den hører til. Et vedtak med
 * flere endringskoder i t_endring gir én periode per kode, som i familie-ef-infotrygd-replika.
 *
 * @param kode endringskode fra t_endring, f.eks. F (førstegangsvedtak), O (opphørt) eller AN (annullert)
 * @param innvilgetFom dato_innv_fom på vedtaket
 * @param innvilgetTom dato_innv_tom på vedtaket. Er den tom, er vedtaket løpende.
 * @param stønadFom fom på delytelsen
 * @param stønadTom tom på delytelsen
 * @param trygdetidOgSats alle registreringer i t_gh for vedtaket, nyeste først
 */
data class PeriodeResponse(
    val personident: String,
    val stønadstype: Stønadstype,
    val sakstype: String,
    val kode: String,
    val brukerId: String,
    val stønadId: Long,
    val vedtakId: Long,
    val vedtakstidspunkt: LocalDateTime,
    val vedtakKodeResultat: String,
    val startDato: LocalDate,
    val innvilgetFom: LocalDate,
    val innvilgetTom: LocalDate?,
    val opphørsdato: LocalDate?,
    val oppdragId: Long?,
    val typeDelytelse: String,
    val typeSats: String,
    val typeUtbetaling: String,
    val stønadFom: LocalDate,
    val stønadTom: LocalDate?,
    val beløp: BigDecimal,
    val trygdetidOgSats: List<TrygdetidOgSatsResponse> = emptyList(),
)

/**
 * Én rad i t_gh. Et vedtak kan ha flere registreringer, og det er ikke avklart hvilken som gjelder for en
 * delytelse, så alle returneres.
 */
data class TrygdetidOgSatsResponse(
    val tidspunktRegistrert: LocalDateTime,
    val trygdetid: Long,
    val satsGs: Long?,
    val satsHs: Long?,
    val brukerId: String,
)
