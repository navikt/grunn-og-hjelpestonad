package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskLoggType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import java.time.Instant

class ProsesseringServiceTest {
    private val prosesseringRepository = mockk<ProsesseringRepository>()
    private val prosesseringService = ProsesseringService(prosesseringRepository, "testnode")

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `rekjor tillates bare for stoppet og avbrutt`(status: Status) {
        every { prosesseringRepository.hentTask(42, true) } returns prosesseringTask(status)
        if (status in setOf(Status.STOPPET, Status.AVBRUTT)) {
            klargjorEndring(Status.KLAR)
            assertThat(prosesseringService.endreTask(42, 7, ProsesseringHandling.REKJOR, "Z123456").status).isEqualTo(Status.KLAR)
        } else {
            assertThatThrownBy {
                prosesseringService.endreTask(42, 7, ProsesseringHandling.REKJOR, "Z123456")
            }.isInstanceOf(ProsesseringApiException::class.java)
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT)
            verify(exactly = 0) { prosesseringRepository.endreStatus(any(), any(), any()) }
        }
    }

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `avbryt og avvik tillates bare for klar og stoppet`(status: Status) {
        every { prosesseringRepository.hentTask(42, true) } returns prosesseringTask(status)
        for (handling in listOf(ProsesseringHandling.AVBRYT, ProsesseringHandling.AVVIK)) {
            if (status in setOf(Status.KLAR, Status.STOPPET)) {
                klargjorEndring(Status.AVBRUTT)
                val oppdatertTask = prosesseringService.endreTask(42, 7, handling, "Z123456", "Undersøkt manuelt")
                assertThat(oppdatertTask.status).isEqualTo(Status.AVBRUTT)
            } else {
                assertThatThrownBy {
                    prosesseringService.endreTask(42, 7, handling, "Z123456", "Undersøkt manuelt")
                }.isInstanceOf(ProsesseringApiException::class.java)
                    .extracting("status")
                    .isEqualTo(HttpStatus.CONFLICT)
            }
        }
    }

    @Test
    fun `utdatert versjon avvises uten skriving`() {
        every { prosesseringRepository.hentTask(42, true) } returns prosesseringTask(Status.STOPPET)
        assertThatThrownBy {
            prosesseringService.endreTask(42, 6, ProsesseringHandling.REKJOR, "Z123456")
        }.hasMessageContaining("Noen andre")
        verify(exactly = 0) { prosesseringRepository.endreStatus(any(), any(), any()) }
    }

    @Test
    fun `ukjent task gir 404 også for logg`() {
        every { prosesseringRepository.hentTask(42, false) } returns null
        assertThatThrownBy { prosesseringService.hentLogg(42, "Z123456") }
            .isInstanceOf(ProsesseringApiException::class.java)
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { prosesseringRepository.hentLogg(any()) }
    }

    @Test
    fun `ugyldig paginering avvises før databaseoppslag`() {
        for (paginering in listOf(0 to 25, 1 to 0, 1 to 101)) {
            assertThatThrownBy {
                prosesseringService.hentTasks(null, null, paginering.first, paginering.second, "Z123456")
            }.isInstanceOf(ProsesseringApiException::class.java)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST)
        }
        verify(exactly = 0) { prosesseringRepository.hentTasks(any(), any(), any(), any()) }
    }

    @Test
    fun `tom avviksmelding avvises uten skriving`() {
        assertThatThrownBy {
            prosesseringService.endreTask(42, 7, ProsesseringHandling.AVVIK, "Z123456", " ")
        }.isInstanceOf(ProsesseringApiException::class.java)
        verify(exactly = 0) { prosesseringRepository.hentTask(any(), any()) }
    }

    @Test
    fun `avvik bruker innlogget ident i begge hendelsene`() {
        every { prosesseringRepository.hentTask(42, true) } returns prosesseringTask(Status.STOPPET)
        klargjorEndring(Status.AVBRUTT)
        prosesseringService.endreTask(42, 7, ProsesseringHandling.AVVIK, "Z123456", "Undersøkt manuelt")
        verify {
            prosesseringRepository.leggTilHendelse(42, TaskLoggType.STATUS_ENDRET, "STOPPET -> AVBRUTT", "Z123456", "testnode")
            prosesseringRepository.leggTilHendelse(42, TaskLoggType.AVVIK, "Undersøkt manuelt", "Z123456", "testnode")
        }
    }

    private fun klargjorEndring(nyStatus: Status) {
        every { prosesseringRepository.endreStatus(42, 7, nyStatus) } returns true
        every { prosesseringRepository.leggTilHendelse(any(), any(), any(), any(), any()) } returns Unit
        every { prosesseringRepository.hentTask(42, false) } returns prosesseringTask(nyStatus).copy(versjon = 8)
    }
}

internal fun prosesseringTask(status: Status = Status.STOPPET): Task =
    Task(
        id = 42,
        type = "TestTask",
        status = status,
        payload = """{"sensitiv":"testverdi"}""",
        metadata = mapOf("traceId" to "test-trace"),
        antallFeil = 3,
        stoppaarsak = null,
        sisteFeilmelding = "Sensitiv feildetalj",
        opprettetTid = Instant.parse("2026-11-18T10:00:00Z"),
        triggerTid = Instant.parse("2026-11-18T11:00:00Z"),
        plukketTid = null,
        sistEndretTid = Instant.parse("2026-11-18T12:00:00Z"),
        versjon = 7,
    )
