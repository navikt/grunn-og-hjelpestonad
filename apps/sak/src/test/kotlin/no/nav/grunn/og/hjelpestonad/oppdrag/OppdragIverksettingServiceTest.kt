package no.nav.grunn.og.hjelpestonad.oppdrag

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class OppdragIverksettingServiceTest {
    private val utbetalingsoppdragService = mockk<UtbetalingsoppdragService>()
    private val oppdragClient = mockk<OppdragClient>()
    private val oppdragRepository = mockk<OppdragRepository>()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = OppdragIverksettingService(utbetalingsoppdragService, oppdragClient, oppdragRepository, meterRegistry)

    private val behandlingId = UUID.randomUUID()
    private val testandel = Testandel(fom = LocalDate.of(2026, 11, 1), tom = LocalDate.of(2026, 11, 30), beløp = 1000)
    private val utbetalingsoppdrag =
        lagUtbetalingsoppdrag(
            Oppdragsgrunnlag(
                behandlingId = behandlingId,
                eksternBehandlingId = 11L,
                eksternFagsakId = 22L,
                personIdent = "12345678910",
                saksbehandlerId = "Z123456",
                vedtaksdato = LocalDate.of(2026, 11, 15),
                andeler = listOf(AndelTilkjentYtelse(beløp = 1000.toBigDecimal(), fom = testandel.fom, tom = testandel.tom, kildeBehandlingId = behandlingId)),
            ),
        ).utbetalingsoppdrag

    private fun oppdragResultat(status: String) = OppdragResultat(behandlingId = behandlingId, fagsakId = UUID.randomUUID(), utbetalingsoppdrag = utbetalingsoppdrag, status = status, andeler = emptySet())

    private fun lagret(
        status: String,
        testandel: Testandel? = null,
    ) {
        every { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, "Z999999") } returns
            LagretUtbetalingsoppdrag(oppdragResultat(status), testandel)
    }

    @BeforeEach
    fun setup() {
        every { oppdragRepository.oppdaterStatus(any(), any(), any()) } just runs
    }

    @Test
    fun `sender nytt oppdrag og setter status LAGT_PÅ_KØ`() {
        lagret(OppdragStatus.OPPRETTET, testandel)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } returns IverksettUtfall.SENDT

        val resultat = service.iverksett(behandlingId, "Z999999")

        assertThat(resultat?.sendt).isTrue()
        assertThat(resultat?.testandel).isEqualTo(testandel)
        verify(exactly = 1) { oppdragClient.iverksett(utbetalingsoppdrag) }
        verify(exactly = 1) { oppdragRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, null) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "ok").count()).isEqualTo(1.0)
    }

    @Test
    fun `409 regnes som sendt og telles som allerede sendt`() {
        lagret(OppdragStatus.OPPRETTET)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } returns IverksettUtfall.ALLEREDE_SENDT

        val resultat = service.iverksett(behandlingId, "Z999999")

        assertThat(resultat?.sendt).isTrue()
        verify(exactly = 1) { oppdragRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, null) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "allerede_sendt").count()).isEqualTo(1.0)
    }

    @Test
    fun `sender lagret oppdrag på nytt uten å overskrive en kvittering`() {
        lagret(OppdragStatus.KVITTERT_OK)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } returns IverksettUtfall.ALLEREDE_SENDT

        service.iverksett(behandlingId, "Z999999")

        verify(exactly = 0) { oppdragRepository.oppdaterStatus(any(), any(), any()) }
    }

    @Test
    fun `sender ingenting når behandlingen ikke endrer utbetalingen`() {
        lagret(OppdragStatus.INGEN_ENDRING)

        val resultat = service.iverksett(behandlingId, "Z999999")

        assertThat(resultat?.sendt).isFalse()
        verify(exactly = 0) { oppdragClient.iverksett(any()) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "ingen_endring").count()).isEqualTo(1.0)
    }

    @Test
    fun `returnerer null når det ikke er noe å sende`() {
        every { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, "Z999999") } returns null

        assertThat(service.iverksett(behandlingId, "Z999999")).isNull()
        verify(exactly = 0) { oppdragClient.iverksett(any()) }
    }

    @Test
    fun `feil fra klienten sendes videre, telles og endrer ikke status`() {
        lagret(OppdragStatus.OPPRETTET)
        val feil = Feil("Oppdragstjenesten er utilgjengelig", HttpStatus.BAD_GATEWAY)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } throws feil

        assertThatThrownBy { service.iverksett(behandlingId, "Z999999") }.isSameAs(feil)
        verify(exactly = 0) { oppdragRepository.oppdaterStatus(any(), any(), any()) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "feilet").count()).isEqualTo(1.0)
    }

    @Test
    fun `henter status med identen oppdraget ble sendt med, og lagrer den på oppdraget`() {
        // Personen har fått ny ident etter at oppdraget ble sendt med 12345678910.
        every { utbetalingsoppdragService.lagOppdragId(behandlingId) } returns
            OppdragId(fagsystem = "IT28", personIdent = "10987654321", behandlingsId = "11")
        val sendtMed = OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "11")
        every { oppdragClient.hentStatus(sendtMed) } returns OppdragStatusSvar(status = "KVITTERT_OK", melding = "Oppdrag behandlet")
        every { oppdragRepository.findById(behandlingId) } returns Optional.of(oppdragResultat(OppdragStatus.LAGT_PÅ_KØ))

        assertThat(service.hentStatus(behandlingId).status).isEqualTo("KVITTERT_OK")
        verify(exactly = 1) { oppdragClient.hentStatus(sendtMed) }
        verify(exactly = 1) { oppdragRepository.oppdaterStatus(behandlingId, "KVITTERT_OK", "Oppdrag behandlet") }
    }

    @Test
    fun `henter status uten å lagre når oppdraget ikke er lagret hos oss`() {
        val oppdragId = OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "11")
        every { utbetalingsoppdragService.lagOppdragId(behandlingId) } returns oppdragId
        every { oppdragClient.hentStatus(oppdragId) } returns OppdragStatusSvar(status = "KVITTERT_OK", melding = null)
        every { oppdragRepository.findById(behandlingId) } returns Optional.empty()

        service.hentStatus(behandlingId)

        verify(exactly = 0) { oppdragRepository.oppdaterStatus(any(), any(), any()) }
    }
}
