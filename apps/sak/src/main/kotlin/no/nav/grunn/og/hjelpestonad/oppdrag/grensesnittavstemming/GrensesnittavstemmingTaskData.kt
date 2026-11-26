package no.nav.grunn.og.hjelpestonad.oppdrag.grensesnittavstemming

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.oppdrag.GrunnOgHjelpFagsystem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Datoene er midnatt i Oppdrags lokale tidsformat. Samme payload og ID brukes ved manuell omkjøring. */
data class GrensesnittavstemmingTaskData(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val avstemmingId: UUID,
) {
    init {
        if (!fraDato.isBefore(tilDato)) {
            throw UgyldigInput("Grensesnittavstemming krever fraDato før tilDato")
        }
    }
}

fun GrensesnittavstemmingTaskData.tilGrensesnittavstemmingRequest(fagsystem: GrunnOgHjelpFagsystem): GrensesnittavstemmingRequest =
    GrensesnittavstemmingRequest(
        fagsystem = fagsystem.kode,
        fra = fraDato.atStartOfDay(),
        til = tilDato.atStartOfDay(),
        avstemmingId = avstemmingId,
    )

/** Beregnes fra lagret sluttgrense, aldri fra kjøretiden, slik at forsinkelser ikke hopper over vinduer. */
fun nesteGrensesnittavstemmingVindu(
    forrigeGrensesnittavstemmingTaskData: GrensesnittavstemmingTaskData,
    nesteAvstemmingId: UUID = UUID.randomUUID(),
): GrensesnittavstemmingTaskData =
    GrensesnittavstemmingTaskData(
        fraDato = forrigeGrensesnittavstemmingTaskData.tilDato,
        tilDato = ukedagPåEllerEtter(forrigeGrensesnittavstemmingTaskData.tilDato.plusDays(1)),
        avstemmingId = nesteAvstemmingId,
    )

/** Ukedager kl. 08 norsk tid. Helligdager hoppes foreløpig ikke over. */
fun tidspunktForGrensesnittavstemming(tilDato: LocalDate): Instant =
    ukedagPåEllerEtter(tilDato)
        .atTime(8, 0)
        .atZone(ZoneId.of("Europe/Oslo"))
        .toInstant()

private fun ukedagPåEllerEtter(dato: LocalDate): LocalDate =
    when (dato.dayOfWeek) {
        DayOfWeek.SATURDAY -> dato.plusDays(2)
        DayOfWeek.SUNDAY -> dato.plusDays(1)
        else -> dato
    }
