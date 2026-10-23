package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.YearMonth

/**
 * 🔴 Rød sone: utvikleren skriver denne selv. Se ADR-0010.
 *
 * Startmåneden er den første måneden ny løsning eier utbetalingen i saken. Oppdrag
 * opphører alle kjeder fra ny startmåned når den er tidligere enn forrige behandlings
 * startmåned. En feil her gir feil opphør i OS.
 *
 * Ren funksjon uten Spring, se ADR-0008. Bruk `UgyldigInput` for input som ikke gir
 * mening.
 *
 * Migrert fra og med kommer som egen parameter når migreringsbehandlingen bygges.
 */
fun utledStartmåned(
    andeler: Set<AndelTilkjentYtelse>,
    forrigeStartmåned: YearMonth?,
): YearMonth {
    val førsteAndelStartMåned = andeler.minOfOrNull { YearMonth.from(it.fom) }
    return listOfNotNull(førsteAndelStartMåned, forrigeStartmåned).minOrNull()
        ?: throw UgyldigInput("Kan ikke utlede startmåned uten andeler eller tidligere startmåned")
}
