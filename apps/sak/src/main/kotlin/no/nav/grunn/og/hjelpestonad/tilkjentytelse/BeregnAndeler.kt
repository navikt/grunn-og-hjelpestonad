package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import java.util.UUID

/** Beholder åpne perioder og desimalbeløp fra utledningen. */
fun lagAndelerTilkjentYtelse(
    utlededeAndeler: List<UtledetAndel>,
    behandlingId: UUID,
): Set<AndelTilkjentYtelse> =
    utlededeAndeler
        .map { utledetAndel ->
            AndelTilkjentYtelse(
                beløp = utledetAndel.beløp,
                fom = utledetAndel.fraOgMedMåned.atDay(1),
                tom = utledetAndel.tilOgMedMåned?.atEndOfMonth(),
                kildeBehandlingId = behandlingId,
            )
        }.toSet()
