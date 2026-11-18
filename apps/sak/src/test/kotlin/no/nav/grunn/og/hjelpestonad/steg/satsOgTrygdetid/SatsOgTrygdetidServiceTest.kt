package no.nav.grunn.og.hjelpestonad.steg.satsogtrygdetid

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.BeregnTilkjentYtelseService
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

class SatsOgTrygdetidServiceTest {
    private val behandlingId = UUID.randomUUID()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val beregnTilkjentYtelseService = mockk<BeregnTilkjentYtelseService>()
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val service = SatsOgTrygdetidService(behandlingService, beregnTilkjentYtelseService, ansvarligSaksbehandlerService, endringshistorikkService)

    @Test
    fun `validerer behandling og ansvar før beregning og registrerer historikk etter beregning`() {
        val andelerTilkjentYtelse = setOf(andel())
        every { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId) } returns andelerTilkjentYtelse

        service.fullførSteg(behandlingId)

        verifyOrder {
            behandlingService.validerBehandlingErRedigerbar(behandlingId)
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId)
            endringshistorikkService.registrerEndring(behandlingId, EndringType.SATS_OG_TRYGDETID_FULLFØRT, "1 andeler, fra 2026-03-01 til 2029-11-30")
        }
    }

    @Test
    fun `registrerer historikk for tom beregning`() {
        every { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId) } returns emptySet()

        service.fullførSteg(behandlingId)

        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.SATS_OG_TRYGDETID_FULLFØRT, "0 andeler") }
    }

    @Test
    fun `registrerer ikke historikk når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws IllegalStateException("Ikke redigerbar")

        assertFailsWith<IllegalStateException> { service.fullførSteg(behandlingId) }

        verify(exactly = 0) { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `registrerer ikke historikk når bruker ikke er ansvarlig`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws IllegalStateException("Ikke ansvarlig")

        assertFailsWith<IllegalStateException> { service.fullførSteg(behandlingId) }

        verify(exactly = 0) { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `registrerer ikke historikk når beregningen feiler`() {
        every { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId) } throws IllegalStateException("Beregning feilet")

        assertFailsWith<IllegalStateException> { service.fullførSteg(behandlingId) }

        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    private fun andel() =
        AndelTilkjentYtelse(
            beløp = BigDecimal("830.85"),
            fom = LocalDate.of(2026, 3, 1),
            tom = LocalDate.of(2029, 11, 30),
            kildeBehandlingId = behandlingId,
        )
}
