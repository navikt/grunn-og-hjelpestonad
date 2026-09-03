package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SimuleringResponse(
    val perioder: List<SimuleringPeriode>,
)

data class SimuleringPeriode(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tom: LocalDate,
    val utbetalinger: List<SimuleringUtbetaling>,
)

data class SimuleringUtbetaling(
    val fagsystem: String,
    val sakId: String,
    val utbetalesTil: Long,
    val stønadstype: String,
    val tidligereUtbetalt: Int,
    val nyttBeløp: Int,
)

data class SimuleringResultatDto(
    val perioder: List<SimuleringPeriodeDto>,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tomSisteUtbetaling: LocalDate,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fomDatoNestePeriode: LocalDate?,
    val etterbetaling: Int,
    val feilutbetaling: Int,
)

data class SimuleringPeriodeDto(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fom: LocalDate,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val tom: LocalDate,
    val nyttBeløp: Int,
    val tidligereUtbetalt: Int,
    val resultat: Int,
    val feilutbetaling: Int,
)

fun SimuleringResponse.tilResultatDto(dagensDato: LocalDate = LocalDate.now()): SimuleringResultatDto {
    val nestePeriodeFom = perioder.firstOrNull { !it.fom.isBefore(dagensDato.withDayOfMonth(1)) }?.fom

    val periodeDtoer =
        perioder.map { periode ->
            val nyttBeløp = periode.utbetalinger.sumOf { it.nyttBeløp }
            val tidligereUtbetalt = periode.utbetalinger.sumOf { it.tidligereUtbetalt }
            val resultat = nyttBeløp - tidligereUtbetalt
            SimuleringPeriodeDto(
                fom = periode.fom,
                tom = periode.tom,
                nyttBeløp = nyttBeløp,
                tidligereUtbetalt = tidligereUtbetalt,
                resultat = resultat,
                feilutbetaling = maxOf(0, tidligereUtbetalt - nyttBeløp),
            )
        }

    val historiskePerioder =
        if (nestePeriodeFom != null) periodeDtoer.filter { it.fom.isBefore(nestePeriodeFom) } else periodeDtoer

    return SimuleringResultatDto(
        perioder = periodeDtoer,
        fom = perioder.minOf { it.fom },
        tomSisteUtbetaling = historiskePerioder.maxOfOrNull { it.tom } ?: perioder.minOf { it.fom },
        fomDatoNestePeriode = nestePeriodeFom,
        etterbetaling = historiskePerioder.filter { it.resultat > 0 }.sumOf { it.resultat },
        feilutbetaling = historiskePerioder.sumOf { it.feilutbetaling },
    )
}

@Table("simulering")
data class Simulering(
    @Id
    val behandlingId: UUID,
    val status: SimuleringStatus,
    val respons: SimuleringResponse? = null,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
)

enum class SimuleringStatus {
    VENTER,
    FERDIG,
    FEILET,
}
