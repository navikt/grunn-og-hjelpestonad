package no.nav.gjenlevende.bs.sak.oppgave

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime

object OppgaveUtil {
    fun lagFristForOppgave(tid: LocalDateTime = LocalDateTime.now()): LocalDate =
        when (tid.dayOfWeek) {
            MONDAY, TUESDAY, WEDNESDAY -> {
                when {
                    tid.erEtter12 -> tid.plusDays(2)
                    else -> tid.plusDays(1)
                }
            }

            THURSDAY -> {
                when {
                    tid.erEtter12 -> tid.finnNeste(MONDAY)
                    else -> tid.plusDays(1)
                }
            }

            FRIDAY -> {
                when {
                    tid.erEtter12 -> tid.finnNeste(TUESDAY)
                    else -> tid.finnNeste(MONDAY)
                }
            }

            SATURDAY, SUNDAY -> {
                tid.finnNeste(TUESDAY)
            }
        }.toLocalDate()

    private val LocalDateTime.erEtter12: Boolean get() = this.hour >= 12

    fun LocalDateTime.finnNeste(dagMål: DayOfWeek): LocalDateTime {
        val idag = this.dayOfWeek.value
        val target = dagMål.value

        val antallDagerViSkalLeggeTil =
            if (target <= idag) {
                7 + (target - idag)
            } else {
                target - idag
            }.toLong()

        return this.plusDays(antallDagerViSkalLeggeTil)
    }
}
