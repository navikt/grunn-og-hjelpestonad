package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.Test

class BehandlingStegServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val behandlingService = BehandlingService(behandlingRepository)
    private val behandlingStegService = BehandlingStegService(behandlingService)

    @Test
    fun `registrerLagretÅrsak setter steget til INNGANGSVILKÅR`() {
        val behandlingId = UUID.randomUUID()
        val oppdatertBehandling = stubHentOgOppdater(behandlingId, gjeldendeSteg = BehandlingSteg.ÅRSAK)

        behandlingStegService.registrerLagretÅrsak(behandlingId)

        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.INNGANGSVILKÅR)
    }

    @Test
    fun `registrerFullførteInngangsvilkår setter SATS_OG_TRYGDETID når det finnes perioder med oppfylte inngangsvilkår`() {
        val behandlingId = UUID.randomUUID()
        val oppdatertBehandling = stubHentOgOppdater(behandlingId, gjeldendeSteg = BehandlingSteg.INNGANGSVILKÅR)

        val nesteSteg = behandlingStegService.registrerFullførteInngangsvilkår(behandlingId, harPerioderMedOppfylteInngangsvilkår = true)

        assertThat(nesteSteg).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
    }

    @Test
    fun `registrerFullførteInngangsvilkår setter VEDTAK_OG_BREV når ingen perioder har oppfylte inngangsvilkår`() {
        val behandlingId = UUID.randomUUID()
        val oppdatertBehandling = stubHentOgOppdater(behandlingId, gjeldendeSteg = BehandlingSteg.INNGANGSVILKÅR)

        val nesteSteg = behandlingStegService.registrerFullførteInngangsvilkår(behandlingId, harPerioderMedOppfylteInngangsvilkår = false)

        assertThat(nesteSteg).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
    }

    @Test
    fun `registrerFullførtSatsOgTrygdetid setter steget til VEDTAK_OG_BREV`() {
        val behandlingId = UUID.randomUUID()
        val oppdatertBehandling = stubHentOgOppdater(behandlingId, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)

        behandlingStegService.registrerFullførtSatsOgTrygdetid(behandlingId)

        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
    }

    @Test
    fun `førTilbakeEtterPeriodeendring flytter steget tilbake når steget er tidligere enn gjeldende steg`() {
        val behandlingId = UUID.randomUUID()
        val oppdatertBehandling = stubHentOgOppdater(behandlingId, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)
        val behandling = lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)

        behandlingStegService.førTilbakeEtterPeriodeendring(behandling, BehandlingSteg.INNGANGSVILKÅR)

        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.INNGANGSVILKÅR)
    }

    @Test
    fun `førTilbakeEtterPeriodeendring lar steget stå når steget er senere enn gjeldende steg`() {
        val behandlingId = UUID.randomUUID()
        val behandling = lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.ÅRSAK)

        behandlingStegService.førTilbakeEtterPeriodeendring(behandling, BehandlingSteg.INNGANGSVILKÅR)

        verify(exactly = 0) { behandlingRepository.update(any()) }
    }

    @Test
    fun `førTilbakeEtterPeriodeendring lar steget stå når steget er det samme som gjeldende steg`() {
        val behandlingId = UUID.randomUUID()
        val behandling = lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.INNGANGSVILKÅR)

        behandlingStegService.førTilbakeEtterPeriodeendring(behandling, BehandlingSteg.INNGANGSVILKÅR)

        verify(exactly = 0) { behandlingRepository.update(any()) }
    }

    @Test
    fun `validerKanEndreSteg kaster feil når steget er senere enn gjeldende steg`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns
            lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)

        assertThatThrownBy { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fullfør «Sats og trygdetid» før du går videre.")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerKanEndreSteg kaster feil når behandlingen ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)

        assertThatThrownBy { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.ÅRSAK) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerKanEndreSteg kaster ikke når steget er nådd`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns
            lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.VEDTAK_OG_BREV)

        assertThatCode { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.SATS_OG_TRYGDETID) }
            .doesNotThrowAnyException()
    }

    private fun stubHentOgOppdater(
        behandlingId: UUID,
        gjeldendeSteg: BehandlingSteg,
    ): CapturingSlot<Behandling> {
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns
            lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = gjeldendeSteg)
        val oppdatertBehandling = slot<Behandling>()
        every { behandlingRepository.update(capture(oppdatertBehandling)) } answers { firstArg() }
        return oppdatertBehandling
    }

    private fun lagBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
        gjeldendeSteg: BehandlingSteg = BehandlingSteg.ÅRSAK,
    ) = Behandling(
        id = behandlingId,
        fagsakId = UUID.randomUUID(),
        status = status,
        resultat = BehandlingResultat.IKKE_SATT,
        gjeldendeSteg = gjeldendeSteg,
    )
}
