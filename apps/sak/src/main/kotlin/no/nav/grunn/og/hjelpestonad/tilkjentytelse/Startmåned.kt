package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.YearMonth

fun utledStartmåned(
    andeler: Set<AndelTilkjentYtelse>,
    forrigeStartmåned: YearMonth?,
): YearMonth {
    val førsteAndelStartMåned = andeler.minOfOrNull { YearMonth.from(it.fom) }
    return listOfNotNull(førsteAndelStartMåned, forrigeStartmåned).minOrNull()
        ?: throw UgyldigInput("Kan ikke utlede startmåned uten andeler eller tidligere startmåned")
}
