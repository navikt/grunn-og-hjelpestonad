package no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.BeregnTilkjentYtelseService
import org.junit.jupiter.api.Test
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
        every { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId) } returns Unit

        service.fullførSteg(behandlingId)

        verifyOrder {
            behandlingService.validerBehandlingErRedigerbar(behandlingId)
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId)
            endringshistorikkService.registrerEndring(behandlingId, EndringType.SATS_OG_TRYGDETID_FULLFØRT, null)
        }
    }

    @Test
    fun `registrerer historikk uten detaljer`() {
        every { beregnTilkjentYtelseService.beregnOgLagreTilkjentYtelse(behandlingId) } returns Unit

        service.fullførSteg(behandlingId)

        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.SATS_OG_TRYGDETID_FULLFØRT, null) }
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
}
