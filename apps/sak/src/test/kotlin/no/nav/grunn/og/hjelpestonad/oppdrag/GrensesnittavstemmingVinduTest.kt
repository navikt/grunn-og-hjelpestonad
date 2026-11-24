package no.nav.grunn.og.hjelpestonad.oppdrag

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GrensesnittavstemmingVinduTest {
    @ParameterizedTest
    @CsvSource(
        "2026-11-12, 2026-11-13",
        "2026-11-13, 2026-11-16",
        "2026-11-14, 2026-11-16",
        "2026-11-15, 2026-11-16",
        "2026-11-16, 2026-11-17",
        "2026-12-24, 2026-12-25",
        "2026-12-31, 2027-01-01",
    )
    fun `neste vindu fortsetter fra sluttgrensen til neste ukedag`(
        forrigeTilDato: LocalDate,
        nesteTilDato: LocalDate,
    ) {
        val forrigeVindu = GrensesnittavstemmingTaskData(forrigeTilDato.minusDays(1), forrigeTilDato, UUID.randomUUID())
        val nesteAvstemmingId = UUID.randomUUID()

        val nesteVindu = nesteGrensesnittavstemmingVindu(forrigeVindu, nesteAvstemmingId)

        assertThat(nesteVindu).isEqualTo(GrensesnittavstemmingTaskData(forrigeTilDato, nesteTilDato, nesteAvstemmingId))
    }

    @ParameterizedTest
    @CsvSource(
        "2026-03-27, 2026-03-27T07:00:00Z",
        "2026-03-28, 2026-03-30T06:00:00Z",
        "2026-03-29, 2026-03-30T06:00:00Z",
        "2026-03-30, 2026-03-30T06:00:00Z",
        "2026-10-23, 2026-10-23T06:00:00Z",
        "2026-10-24, 2026-10-26T07:00:00Z",
        "2026-10-25, 2026-10-26T07:00:00Z",
    )
    fun `kjøring er klokken åtte norsk tid også over helg og sommertid`(
        tilDato: LocalDate,
        forventetTidspunkt: Instant,
    ) {
        assertThat(tidspunktForGrensesnittavstemming(tilDato)).isEqualTo(forventetTidspunkt)
    }

    @ParameterizedTest
    @CsvSource("2026-11-16, 2026-11-16", "2026-11-17, 2026-11-16")
    fun `tomt eller omvendt vindu avvises`(
        fraDato: LocalDate,
        tilDato: LocalDate,
    ) {
        assertThatThrownBy { GrensesnittavstemmingTaskData(fraDato, tilDato, UUID.randomUUID()) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `lagret payload bevarer datoer og avstemmings-ID ved deserialisering`() {
        val avstemmingsvindu = GrensesnittavstemmingTaskData(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 16), UUID.randomUUID())

        val serialisertPayload = GrensesnittavstemmingTask.TYPE.serialiser(avstemmingsvindu)

        assertThat(GrensesnittavstemmingTask.TYPE.deserialiser(serialisertPayload)).isEqualTo(avstemmingsvindu)
    }
}
