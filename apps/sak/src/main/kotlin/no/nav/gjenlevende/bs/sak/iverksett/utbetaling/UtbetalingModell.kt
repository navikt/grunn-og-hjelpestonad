package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// TODO utgangspunkt i historisk. Må endres etter koordinasjon med helved
data class UtbetalingMelding(
    val behandlingId: UUID,
    val sakId: String,
    val personident: String,
    val stønad: String,
    val vedtakstidspunkt: LocalDateTime,
    val periodetype: Periodetype = Periodetype.MND,
    val perioder: List<Periode>,
    val saksbehandler: String,
    val beslutter: String,
    val dryrun: Boolean = true,
)

data class Periode(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tom: LocalDate,
    val beløp: Int,
)

enum class Periodetype {
    DAG,
    UKEDAG,
    MND,
    EN_GANG,
}

data class UtbetalingStatusMelding(
    val status: StatusType,
    val error: StatusError? = null,
)

enum class StatusType {
    OK,
    FEILET,
    MOTTATT,
    HOS_OPPDRAG,
}

data class StatusError(
    val statusCode: Int,
    val msg: String,
    val doc: String,
)
