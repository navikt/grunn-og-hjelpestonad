package no.nav.grunn.og.hjelpestonad.behandling

import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Stegflyten i en behandling: sjekker om et steg kan endres, og registrerer når et steg er fullført.
 */
@Service
class BehandlingStegService(
    private val behandlingService: BehandlingService,
) {
    /**
     * Sjekker at saksbehandler kan endre eller fullføre [steg]. Behandlingen må være redigerbar, og steget
     * må være nådd. Trenger du behandlingen etterpå, hent den og bruk `Behandling.validerKanEndre`.
     */
    fun validerKanEndreSteg(
        behandlingId: UUID,
        steg: BehandlingSteg,
    ) = behandlingService.hentBehandlingEllerKast(behandlingId).validerKanEndre(steg)

    /**
     * For operasjoner som bare leser, som simulering. Sjekker steget bare mens behandlingen er redigerbar.
     * Etter sending til beslutter kan ikke grunnlaget endres, og beslutter skal kunne se alt.
     */
    fun validerHarNåddStegHvisRedigerbar(
        behandlingId: UUID,
        steg: BehandlingSteg,
    ) {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        if (behandling.erRedigerbar()) behandling.validerHarNådd(steg)
    }

    fun registrerLagretÅrsak(behandlingId: UUID) {
        settGjeldendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)
    }

    /**
     * Returnerer steget behandlingen går til.
     */
    fun registrerFullførteInngangsvilkår(
        behandlingId: UUID,
        harPerioderMedOppfylteInngangsvilkår: Boolean,
    ): BehandlingSteg {
        val stegEtterInngangsvilkår = utledStegEtterInngangsvilkår(harPerioderMedOppfylteInngangsvilkår)
        settGjeldendeSteg(behandlingId, stegEtterInngangsvilkår)
        return stegEtterInngangsvilkår
    }

    fun registrerFullførtSatsOgTrygdetid(behandlingId: UUID) {
        settGjeldendeSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
    }

    /**
     * Behandlingen må fullføre [steg] på nytt. Står den allerede tidligere i flyten, blir den stående.
     * Sammenligner med [behandling] slik den var da endringen startet.
     */
    fun førTilbakeEtterPeriodeendring(
        behandling: Behandling,
        steg: BehandlingSteg,
    ) {
        if (steg.rekkefølge < behandling.gjeldendeSteg.rekkefølge) settGjeldendeSteg(behandling.id, steg)
    }

    private fun settGjeldendeSteg(
        behandlingId: UUID,
        gjeldendeSteg: BehandlingSteg,
    ) {
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        behandlingService.oppdater(behandling.copy(gjeldendeSteg = gjeldendeSteg))
    }
}
