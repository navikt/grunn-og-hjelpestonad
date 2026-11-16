package no.nav.grunn.og.hjelpestonad.vilkår.medlemskap

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.periode.Endringstyper
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårMedlemskapService(
    vilkårMedlemskapRepository: VilkårMedlemskapRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : PeriodeMedBegrunnelseService<VilkårMedlemskap, VilkårMedlemskapRequest>(
        vilkårMedlemskapRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
        Endringstyper(
            opprettet = EndringType.VILKÅR_VURDERING_OPPRETTET,
            oppdatert = EndringType.VILKÅR_VURDERING_OPPDATERT,
            slettet = EndringType.VILKÅR_VURDERING_SLETTET,
        ),
    ) {
    private val vilkårType = VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN

    override val periodebeskrivelse = "vilkårsvurdering for ${vilkårType.name}"

    /** Bare perioder vurdert til ja kopieres. */
    override fun skalKopieres(periode: VilkårMedlemskap) = periode.erVilkårOppfylt()

    override fun endringsdetalj(periode: VilkårMedlemskap) = "${vilkårType.name}, Vurdering: ${periode.vurdering.name}"

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

    override fun VilkårMedlemskap.oppdaterMed(
        request: VilkårMedlemskapRequest,
    ) = copy(
        regelverk = request.regelverk,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )
}
