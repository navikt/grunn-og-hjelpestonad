package no.nav.gjenlevende.bs.sak.oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

class OppgaveDtoUtilTest {
    @Test
    fun `Sjekk at frist er riktig gammel`() {
        for (dag in DayOfWeek.entries) {
            when (dag) {
                MONDAY, TUESDAY, WEDNESDAY -> {
                    OppgaveUtil.lagFristForOppgave(dag.dato().før12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(dag.plus(1))
                    }
                    OppgaveUtil.lagFristForOppgave(dag.dato().etter12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(dag.plus(2))
                    }
                }

                THURSDAY -> {
                    OppgaveUtil.lagFristForOppgave(dag.dato().før12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(FRIDAY)
                    }
                    OppgaveUtil.lagFristForOppgave(dag.dato().etter12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(MONDAY)
                    }
                }

                FRIDAY -> {
                    OppgaveUtil.lagFristForOppgave(dag.dato().før12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(MONDAY)
                    }
                    OppgaveUtil.lagFristForOppgave(dag.dato().etter12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(TUESDAY)
                    }
                }

                SATURDAY, SUNDAY -> {
                    OppgaveUtil.lagFristForOppgave(dag.dato().før12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(TUESDAY)
                    }
                    OppgaveUtil.lagFristForOppgave(dag.dato().etter12()).let { frist ->
                        assertThat(frist.dayOfWeek).isEqualTo(TUESDAY)
                    }
                }
            }
        }
    }
}

private fun DayOfWeek.dato() =
    LocalDateTime
        .now()
        .with(TemporalAdjusters.next(this))

private fun LocalDateTime.før12(): LocalDateTime = this.withHour(11).withMinute(59).withSecond(59)

private fun LocalDateTime.etter12(): LocalDateTime = this.withHour(12).withMinute(0).withSecond(1)
