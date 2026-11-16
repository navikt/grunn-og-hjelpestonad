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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class OppdragIverksettingServiceTest {
    private val utbetalingsoppdragService = mockk<UtbetalingsoppdragService>()
    private val oppdragClient = mockk<OppdragClient>()
    private val oppdragResultatRepository = mockk<OppdragResultatRepository>()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = OppdragIverksettingService(utbetalingsoppdragService, oppdragClient, oppdragResultatRepository, meterRegistry)

    private val behandlingId = UUID.randomUUID()
    private val fagsakId = UUID.randomUUID()
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

    private fun oppdragResultat(status: String) = OppdragResultat(behandlingId = behandlingId, fagsakId = fagsakId, utbetalingsoppdrag = utbetalingsoppdrag, status = status, andeler = emptySet())

    private fun lagret(
        status: String,
        testandel: Testandel? = null,
    ) {
        every { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, "Z999999") } returns
            LagretUtbetalingsoppdrag(oppdragResultat(status), testandel)
    }

    @BeforeEach
    fun setup() {
        every { oppdragResultatRepository.oppdaterStatus(any(), any(), any()) } just runs
    }

    @Test
    fun `sender nytt oppdrag og setter status LAGT_PÅ_KØ`() {
        lagret(OppdragStatus.OPPRETTET, testandel)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } returns IverksettUtfall.SENDT

        val iverksettingsresultat = service.iverksett(behandlingId, "Z999999")

        assertThat(iverksettingsresultat).isEqualTo(Iverksettingsresultat.Sendt(oppdragResultat(OppdragStatus.OPPRETTET), testandel))
        verify(exactly = 1) { oppdragClient.iverksett(utbetalingsoppdrag) }
        verify(exactly = 1) { oppdragResultatRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, null) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "ok").count()).isEqualTo(1.0)
    }

    @Test
    fun `409 regnes som sendt og telles som allerede sendt`() {
        lagret(OppdragStatus.OPPRETTET)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } returns IverksettUtfall.ALLEREDE_SENDT

        val iverksettingsresultat = service.iverksett(behandlingId, "Z999999")

        assertThat(iverksettingsresultat).isInstanceOf(Iverksettingsresultat.Sendt::class.java)
        verify(exactly = 1) { oppdragResultatRepository.oppdaterStatus(behandlingId, OppdragStatus.LAGT_PÅ_KØ, null) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "allerede_sendt").count()).isEqualTo(1.0)
    }

    @ParameterizedTest
    @ValueSource(strings = [OppdragStatus.LAGT_PÅ_KØ, OppdragStatus.KVITTERT_OK, "KVITTERT_FUNKSJONELL_FEIL"])
    fun `sender ikke oppdraget på nytt når OS har tatt imot det`(status: String) {
        lagret(status)

        val iverksettingsresultat = service.iverksett(behandlingId, "Z999999")

        assertThat(iverksettingsresultat).isEqualTo(Iverksettingsresultat.AlleredeIverksatt(oppdragResultat(status)))
        verify(exactly = 0) { oppdragClient.iverksett(any()) }
        verify(exactly = 0) { oppdragResultatRepository.oppdaterStatus(any(), any(), any()) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "allerede_iverksatt").count()).isEqualTo(1.0)
    }

    @Test
    fun `sender ingenting når behandlingen ikke endrer utbetalingen`() {
        lagret(OppdragStatus.INGEN_ENDRING)

        val iverksettingsresultat = service.iverksett(behandlingId, "Z999999")

        assertThat(iverksettingsresultat).isEqualTo(Iverksettingsresultat.IngenEndring(oppdragResultat(OppdragStatus.INGEN_ENDRING), testandel = null))
        verify(exactly = 0) { oppdragClient.iverksett(any()) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "ingen_endring").count()).isEqualTo(1.0)
    }

    @Test
    fun `returnerer IngentingÅSende når det ikke er noe å sende`() {
        every { utbetalingsoppdragService.lagOgLagreUtbetalingsoppdrag(behandlingId, "Z999999") } returns null

        assertThat(service.iverksett(behandlingId, "Z999999")).isEqualTo(Iverksettingsresultat.IngentingÅSende)
        verify(exactly = 0) { oppdragClient.iverksett(any()) }
    }

    @Test
    fun `feil fra klienten sendes videre, telles og endrer ikke status`() {
        lagret(OppdragStatus.OPPRETTET)
        val feil = Feil("Oppdragstjenesten er utilgjengelig", HttpStatus.BAD_GATEWAY)
        every { oppdragClient.iverksett(utbetalingsoppdrag) } throws feil

        assertThatThrownBy { service.iverksett(behandlingId, "Z999999") }.isSameAs(feil)
        verify(exactly = 0) { oppdragResultatRepository.oppdaterStatus(any(), any(), any()) }
        assertThat(meterRegistry.counter("oppdrag_iverksetting", "resultat", "feilet").count()).isEqualTo(1.0)
    }

    @Test
    fun `henter status med identen oppdraget ble sendt med, og lagrer den på oppdraget`() {
        // Personen har fått ny ident etter at oppdraget ble sendt med 12345678910.
        every { utbetalingsoppdragService.lagOppdragId(behandlingId) } returns
            OppdragId(fagsystem = "IT28", personIdent = "10987654321", behandlingsId = "11")
        val sendtMed = OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "11")
        every { oppdragClient.hentStatus(sendtMed) } returns OppdragStatusSvar(status = "KVITTERT_OK", melding = "Oppdrag behandlet")
        every { oppdragResultatRepository.findById(behandlingId) } returns Optional.of(oppdragResultat(OppdragStatus.LAGT_PÅ_KØ))

        assertThat(service.hentStatus(behandlingId).status).isEqualTo("KVITTERT_OK")
        verify(exactly = 1) { oppdragClient.hentStatus(sendtMed) }
        verify(exactly = 1) { oppdragResultatRepository.oppdaterStatus(behandlingId, "KVITTERT_OK", "Oppdrag behandlet") }
    }

    @Test
    fun `henter status uten å lagre når oppdraget ikke er lagret hos oss`() {
        val oppdragId = OppdragId(fagsystem = "IT28", personIdent = "12345678910", behandlingsId = "11")
        every { utbetalingsoppdragService.lagOppdragId(behandlingId) } returns oppdragId
        every { oppdragClient.hentStatus(oppdragId) } returns OppdragStatusSvar(status = "KVITTERT_OK", melding = null)
        every { oppdragResultatRepository.findById(behandlingId) } returns Optional.empty()

        service.hentStatus(behandlingId)

        verify(exactly = 0) { oppdragResultatRepository.oppdaterStatus(any(), any(), any()) }
    }
}
