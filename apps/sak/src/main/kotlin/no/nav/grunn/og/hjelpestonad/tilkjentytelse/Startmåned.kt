package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.YearMonth

fun utledStartmåned(
    andeler: Set<AndelTilkjentYtelse>,
    forrigeStartmåned: YearMonth?,
    opphørsmåned: YearMonth?,
): YearMonth {
    val erOpphør = opphørsmåned != null

    val startmåned =
        if (erOpphør) {
            listOfNotNull(opphørsmåned, forrigeStartmåned).min()
        } else {
            val førsteMånedMedAndeler = andeler.minOfOrNull { YearMonth.from(it.fom) }
            listOfNotNull(førsteMånedMedAndeler, forrigeStartmåned).minOrNull()
        }

    return startmåned ?: throw UgyldigInput("Kan ikke utlede startmåned for tilkjent ytelse. Ingen andeler og ingen forrige startmåned.")
}
