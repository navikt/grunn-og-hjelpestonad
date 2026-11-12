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
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

class OppdragIverksettingServiceTest {
    private val utbetalingsoppdragService = mockk<UtbetalingsoppdragService>()
    private val oppdragClient = mockk<OppdragClient>()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = OppdragIverksettingService(utbetalingsoppdragService, oppdragClient, meterRegistry)

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
                andelerTilkjentYtelse = listOf(AndelTilkjentYtelse(beløp = 1000.toBigDecimal(), fom = testandel.fom, tom = testandel.tom, kildeBehandlingId = behandlingId)),
            ),
        ).utbetalingsoppdrag

    @Test
    fun `iverksetter oppdraget og svarer med saksnummer og behandlings-ID fra oppdraget`() {
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId) } returns UtbetalingsoppdragForBehandling(utbetalingsoppdrag, testandel)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } just runs

        val respons = service.iverksett(behandlingId)

        assertThat(respons).isEqualTo(OppdragIverksettingResponse(saksnummer = "22", behandlingsId = "11", antallPerioder = 1, testandel = testandel))
        verify(exactly = 1) { oppdragClient.iverksett(utbetalingsoppdrag) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "ok").count()).isEqualTo(1.0)
    }

    @Test
    fun `409 fra klienten sendes videre og telles som allerede sendt`() {
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId) } returns UtbetalingsoppdragForBehandling(utbetalingsoppdrag, null)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } throws Feil("Oppdrag er allerede sendt", HttpStatus.CONFLICT)

        assertThatThrownBy { service.iverksett(behandlingId) }
            .isInstanceOfSatisfying(Feil::class.java) { assertThat(it.httpStatus).isEqualTo(HttpStatus.CONFLICT) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "allerede_sendt").count()).isEqualTo(1.0)
    }

    @Test
    fun `annen feil fra klienten sendes videre og telles som feilet`() {
        every { utbetalingsoppdragService.lagUtbetalingsoppdrag(behandlingId) } returns UtbetalingsoppdragForBehandling(utbetalingsoppdrag, null)
        val feil = Feil("Oppdragstjenesten er utilgjengelig", HttpStatus.BAD_GATEWAY)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } throws feil

        assertThatThrownBy { service.iverksett(behandlingId) }.isSameAs(feil)
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "feilet").count()).isEqualTo(1.0)
    }

    @Test
    fun `henter status med OppdragId for behandlingen`() {
        val oppdragId = OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "11")
        every { utbetalingsoppdragService.lagOppdragId(behandlingId) } returns oppdragId
        every { oppdragClient.hentStatus(oppdragId) } returns OppdragStatusSvar(status = "KVITTERT_OK", melding = null)

        assertThat(service.hentStatus(behandlingId).status).isEqualTo("KVITTERT_OK")
    }
}
