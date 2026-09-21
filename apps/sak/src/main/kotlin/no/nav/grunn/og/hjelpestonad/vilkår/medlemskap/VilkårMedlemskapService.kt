package no.nav.grunn.og.hjelpestonad.vilkår.medlemskap

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårMedlemskapService(
    vilkårMedlemskapRepository: VilkårMedlemskapRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : VilkårPeriodeService<VilkårMedlemskap, VilkårMedlemskapRequest>(
        vilkårMedlemskapRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
    ) {
    override val vilkårType = VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN

    override fun nyPeriode(
        behandlingId: UUID,
        request: VilkårMedlemskapRequest,
    ) = VilkårMedlemskap(
        behandlingId = behandlingId,
        regelverk = request.regelverk,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    override fun oppdatertPeriode(
        eksisterende: VilkårMedlemskap,
        request: VilkårMedlemskapRequest,
    ) = eksisterende.copy(
        regelverk = request.regelverk,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )
}
