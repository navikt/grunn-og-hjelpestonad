package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.periode.formaterPeriode
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class InngangsvilkårResultat(
    val perioder: List<PeriodeMedOppfylteInngangsvilkår>,
    val nesteSteg: NesteStegEtterInngangsvilkår,
)

@Service
class InngangsvilkårStegService(
    private val vilkårMedlemskapService: VilkårMedlemskapService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val behandlingService: BehandlingService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val endringshistorikkService: EndringshistorikkService,
) {
    @Transactional
    fun fullførSteg(behandlingId: UUID): InngangsvilkårResultat {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)

        val vurderteInngangsvilkår = hentVurderteInngangsvilkår(behandlingId)
        val feil = VilkårVurderingValidering.valider(vurderteInngangsvilkår)
        if (feil.isNotEmpty()) throw VilkårValideringFeil(feil)

        val perioder = PerioderMedOppfylteInngangsvilkårUtleder.utled(vurderteInngangsvilkår)
        registrerEndringshistorikk(behandlingId, perioder)
        return InngangsvilkårResultat(perioder = perioder, nesteSteg = NesteStegEtterInngangsvilkår.utled(perioder))
    }

    fun hentPerioder(behandlingId: UUID): List<PeriodeMedOppfylteInngangsvilkår> = PerioderMedOppfylteInngangsvilkårUtleder.utled(hentVurderteInngangsvilkår(behandlingId))

    private fun hentVurderteInngangsvilkår(behandlingId: UUID) =
        VurderteInngangsvilkår(
            behandlingId = behandlingId,
            medlemskap = vilkårMedlemskapService.hentPerioder(behandlingId),
            diagnoser = vilkårDiagnoseService.hentPerioder(behandlingId),
        )

    private fun registrerEndringshistorikk(
        behandlingId: UUID,
        perioder: List<PeriodeMedOppfylteInngangsvilkår>,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = EndringType.INNGANGSVILKÅR_VURDERING_FULLFØRT,
        detaljer =
            if (perioder.isEmpty()) {
                "Ingen perioder med oppfylte inngangsvilkår"
            } else {
                "${perioder.size} ${if (perioder.size == 1) "periode" else "perioder"} med oppfylte inngangsvilkår: " +
                    perioder.joinToString(", ") { formaterPeriode(it) }
            },
    )
}
