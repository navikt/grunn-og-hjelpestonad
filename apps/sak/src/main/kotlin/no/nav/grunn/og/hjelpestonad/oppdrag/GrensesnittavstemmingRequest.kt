package no.nav.grunn.og.hjelpestonad.oppdrag

import java.time.LocalDateTime
import java.util.UUID

/** Kontrakten i familie-kontrakter. Mottakeren velger oppdrag med avstemmingstidspunkt i `[fra, til)`. */
data class GrensesnittavstemmingRequest(
    val fagsystem: String,
    val fra: LocalDateTime,
    val til: LocalDateTime,
    val avstemmingId: UUID,
)
