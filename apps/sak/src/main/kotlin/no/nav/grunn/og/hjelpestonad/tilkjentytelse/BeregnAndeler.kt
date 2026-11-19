package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.time.YearMonth
import java.util.UUID

/** Fyller manglende sluttdato med beregningsmåneden pluss tre år. Eksplisitte sluttdatoer beholdes. */
fun fyllInnManglendeSluttdato(
    utlededeAndeler: List<UtledetAndel>,
    beregningsmåned: YearMonth,
    behandlingId: UUID,
): Set<AndelTilkjentYtelse> {
    val standardSluttmåned = beregningsmåned.plusYears(3)
    return utlededeAndeler
        .map { utledetAndel ->
            val sluttmåned = utledetAndel.tilOgMedMåned ?: standardSluttmåned
            if (sluttmåned < utledetAndel.fraOgMedMåned) {
                throw UgyldigInput("Perioden starter etter standard sluttdato på tre år frem. Fyll ut en til og med-dato for perioden.")
            }
            AndelTilkjentYtelse(
                beløp = utledetAndel.beløp,
                fom = utledetAndel.fraOgMedMåned.atDay(1),
                tom = sluttmåned.atEndOfMonth(),
                kildeBehandlingId = behandlingId,
            )
        }.toSet()
}
