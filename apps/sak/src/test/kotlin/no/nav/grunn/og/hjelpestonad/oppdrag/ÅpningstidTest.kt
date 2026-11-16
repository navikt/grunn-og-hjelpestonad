package no.nav.grunn.og.hjelpestonad.oppdrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class ÅpningstidTest {
    // 2026-11-09 er en mandag, 2026-11-13 en fredag.
    @ParameterizedTest(name = "{0} + {1} min -> {2}")
    @CsvSource(
        "2026-11-09T13:00, 15, 2026-11-09T13:15",
        "2026-11-09T20:45, 15, 2026-11-09T21:00",
        "2026-11-09T20:50, 15, 2026-11-10T06:00",
        "2026-11-09T23:30, 15, 2026-11-10T06:00",
        "2026-11-10T03:00, 15, 2026-11-10T06:00",
        "2026-11-10T05:50, 15, 2026-11-10T06:05",
        "2026-11-13T20:50, 15, 2026-11-16T06:00",
        "2026-11-14T12:00, 15, 2026-11-16T06:00",
        "2026-11-15T23:50, 15, 2026-11-16T06:00",
        // Sommertid slutter 25. oktober 2026: 06:00 skal være norsk tid også på begge sider av overgangen.
        "2026-10-24T22:00, 15, 2026-10-26T06:00",
    )
    fun `neste tidspunkt er på en hverdag mellom 06 og 21 norsk tid`(
        nåINorskTid: String,
        forsinkelseIMinutter: Long,
        forventetTidspunkt: String,
    ) {
        val nesteTidspunkt = nesteTidspunktIÅpningstid(norskTidTilInstant(nåINorskTid), Duration.ofMinutes(forsinkelseIMinutter))

        assertThat(nesteTidspunkt).isEqualTo(norskTidTilInstant(forventetTidspunkt))
    }

    private fun norskTidTilInstant(datoOgKlokkeslett: String) =
        LocalDateTime
            .parse(datoOgKlokkeslett)
            .atZone(ZoneId.of("Europe/Oslo"))
            .toInstant()
}
