package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Simuleringen fra Oppdrag oppsummert for hele perioden og per måned, regnet ut som i ba-sak (ADR-0018).
 *
 * [feilutbetaling], [etterbetaling] og [tomSisteUtbetaling] gjelder bare månedene før [nesteUtbetaling]. [perioder]
 * stopper ved [nesteUtbetaling], så måneder etter den vises ikke.
 */
data class Simuleringsoppsummering(
    val fom: LocalDate?,
    val tomSisteUtbetaling: LocalDate?,
    val feilutbetaling: BigDecimal,
    val etterbetaling: BigDecimal,
    val nesteUtbetaling: NesteUtbetaling?,
    /** Fagområdekoder fra Oppdrag som ikke er grunn- eller hjelpestønad, sortert på navn. Manuelle grunn- og hjelpestønadskoder regnes ikke som ukjente, se [SimuleringOppsummerer.MANUELLE_FAGOMRÅDEKODER]. */
    val ukjenteFagområdekoder: List<String>,
    val perioder: List<Simuleringsperiode>,
)

data class NesteUtbetaling(
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
    val beløp: BigDecimal,
)

/** Én eller flere påfølgende måneder med like beløp. Neste utbetaling står alltid alene. */
data class Simuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallMåneder: Int,
    val erNesteUtbetaling: Boolean,
    val beløpPerMåned: Simuleringsbeløp,
    val sumForPerioden: Simuleringsbeløp,
)

data class Simuleringsbeløp(
    val nyttBeløp: BigDecimal,
    val tidligereUtbetalt: BigDecimal,
    val resultat: BigDecimal,
    val feilutbetaling: BigDecimal,
    val etterbetaling: BigDecimal,
    val manuellPostering: BigDecimal,
) {
    /** Sammenligner beløpene uten å se på skala, så 752 og 752.00 er like. */
    fun erLik(andreSimuleringsbeløp: Simuleringsbeløp): Boolean =
        nyttBeløp.compareTo(andreSimuleringsbeløp.nyttBeløp) == 0 &&
            tidligereUtbetalt.compareTo(andreSimuleringsbeløp.tidligereUtbetalt) == 0 &&
            resultat.compareTo(andreSimuleringsbeløp.resultat) == 0 &&
            feilutbetaling.compareTo(andreSimuleringsbeløp.feilutbetaling) == 0 &&
            etterbetaling.compareTo(andreSimuleringsbeløp.etterbetaling) == 0 &&
            manuellPostering.compareTo(andreSimuleringsbeløp.manuellPostering) == 0

    operator fun plus(andreSimuleringsbeløp: Simuleringsbeløp) =
        Simuleringsbeløp(
            nyttBeløp = nyttBeløp + andreSimuleringsbeløp.nyttBeløp,
            tidligereUtbetalt = tidligereUtbetalt + andreSimuleringsbeløp.tidligereUtbetalt,
            resultat = resultat + andreSimuleringsbeløp.resultat,
            feilutbetaling = feilutbetaling + andreSimuleringsbeløp.feilutbetaling,
            etterbetaling = etterbetaling + andreSimuleringsbeløp.etterbetaling,
            manuellPostering = manuellPostering + andreSimuleringsbeløp.manuellPostering,
        )

    /** Beløpene for [antallMåneder] like måneder. */
    operator fun times(antallMåneder: Int): Simuleringsbeløp {
        val antallMånederSomBigDecimal = BigDecimal.valueOf(antallMåneder.toLong())
        return Simuleringsbeløp(
            nyttBeløp = nyttBeløp * antallMånederSomBigDecimal,
            tidligereUtbetalt = tidligereUtbetalt * antallMånederSomBigDecimal,
            resultat = resultat * antallMånederSomBigDecimal,
            feilutbetaling = feilutbetaling * antallMånederSomBigDecimal,
            etterbetaling = etterbetaling * antallMånederSomBigDecimal,
            manuellPostering = manuellPostering * antallMånederSomBigDecimal,
        )
    }

    companion object {
        val NULL =
            Simuleringsbeløp(
                nyttBeløp = BigDecimal.ZERO,
                tidligereUtbetalt = BigDecimal.ZERO,
                resultat = BigDecimal.ZERO,
                feilutbetaling = BigDecimal.ZERO,
                etterbetaling = BigDecimal.ZERO,
                manuellPostering = BigDecimal.ZERO,
            )
    }
}

/** En postering fra Oppdrag med de påkrevde feltene sjekket. Uten `mottakerNummer`, som er fødselsnummeret. */
data class Simuleringspostering(
    val fagområdekode: String,
    val mottakertype: Mottakertype?,
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
    val posteringstype: Posteringstype,
    val betalingstype: Betalingstype?,
    val klassekode: String?,
    val beløp: BigDecimal,
    val erFeilkonto: Boolean?,
    val utenInntrekk: Boolean?,
)

/**
 * Speiler `PosteringType` i familie-kontrakter (`no.nav.familie.kontrakter.felles.simulering`). familie-oppdrag sender
 * navnet på verdien, ikke koden.
 */
enum class Posteringstype { YTELSE, FEILUTBETALING, FORSKUDSSKATT, JUSTERING, TREKK, MOTP }

/**
 * Speiler `BetalingType` i familie-kontrakter (`no.nav.familie.kontrakter.felles.simulering`). familie-oppdrag sender
 * navnet på verdien, ikke koden.
 */
enum class Betalingstype { DEBIT, KREDIT }

/**
 * Speiler `MottakerType` i familie-kontrakter (`no.nav.familie.kontrakter.felles.simulering`). familie-oppdrag sender
 * navnet på verdien.
 */
enum class Mottakertype { BRUKER, ARBG_ORG, ARBG_PRIV }

/** Alle beløpsgivende posteringer med samme `fom`, med beløpene for måneden. */
data class Simuleringsmåned(
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
    val beløp: Simuleringsbeløp,
    val posteringer: List<Simuleringspostering>,
)
