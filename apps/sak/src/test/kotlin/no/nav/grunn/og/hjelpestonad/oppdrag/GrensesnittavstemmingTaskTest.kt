package no.nav.grunn.og.hjelpestonad.oppdrag

import efterlatte.prosessering.TaskKontekst
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GrensesnittavstemmingTaskTest {
    private val oppdragClient = mockk<OppdragClient>()
    private val avstemmingsvindu =
        GrensesnittavstemmingTaskData(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13), UUID.randomUUID())
    private val kontekst =
        mockk<TaskKontekst<GrensesnittavstemmingTaskData>>(relaxed = true) {
            every { payload } returns avstemmingsvindu
        }

    private fun task() = GrensesnittavstemmingTask(oppdragClient)

    @Test
    fun `sender lagret vindu og ID før neste task opprettes`() {
        every { oppdragClient.grensesnittavstem(any()) } just runs
        val nesteAvstemmingsvindu = slot<GrensesnittavstemmingTaskData>()

        task().utfor(kontekst)

        verifyOrder {
            oppdragClient.grensesnittavstem(
                GrensesnittavstemmingRequest("IT28", avstemmingsvindu.fraDato.atStartOfDay(), avstemmingsvindu.tilDato.atStartOfDay(), avstemmingsvindu.avstemmingId),
            )
            kontekst.opprettNesteTask(GrensesnittavstemmingTask.TYPE, capture(nesteAvstemmingsvindu), Instant.parse("2026-11-16T07:00:00Z"))
        }
        assertThat(nesteAvstemmingsvindu.captured.fraDato).isEqualTo(avstemmingsvindu.tilDato)
        assertThat(nesteAvstemmingsvindu.captured.tilDato).isEqualTo(LocalDate.of(2026, 11, 16))
        assertThat(nesteAvstemmingsvindu.captured.avstemmingId).isNotEqualTo(avstemmingsvindu.avstemmingId)
    }

    @Test
    fun `feilet sending kastes videre uten å opprette neste task og uten automatisk retry`() {
        every { oppdragClient.grensesnittavstem(any()) } throws IllegalStateException("Ukjent leveringsstatus")

        assertThatThrownBy { task().utfor(kontekst) }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 0) { kontekst.opprettNesteTask<GrensesnittavstemmingTaskData>(any(), any(), any()) }
        assertThat(task().retry.maxAntallFeil).isEqualTo(1)
    }

    @Test
    fun `nytt forsøk etter usikker sending beholder vindu og ID`() {
        every { oppdragClient.grensesnittavstem(any()) } throws IllegalStateException("Ukjent leveringsstatus")
        assertThatThrownBy { task().utfor(kontekst) }.isInstanceOf(IllegalStateException::class.java)
        every { oppdragClient.grensesnittavstem(any()) } just runs

        task().utfor(kontekst)

        verify(exactly = 2) {
            oppdragClient.grensesnittavstem(
                GrensesnittavstemmingRequest("IT28", avstemmingsvindu.fraDato.atStartOfDay(), avstemmingsvindu.tilDato.atStartOfDay(), avstemmingsvindu.avstemmingId),
            )
        }
        verify(exactly = 1) { kontekst.opprettNesteTask<GrensesnittavstemmingTaskData>(any(), any(), any()) }
    }
}
