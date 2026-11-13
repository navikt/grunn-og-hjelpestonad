package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.periode.Endringstyper
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårEkstrautgifterService(
    vilkårEkstrautgifterRepository: VilkårEkstrautgifterRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
) : PeriodeMedBegrunnelseService<VilkårEkstrautgifter, VilkårEkstrautgifterRequest>(
        vilkårEkstrautgifterRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
        Endringstyper(
            opprettet = EndringType.VILKÅR_VURDERING_OPPRETTET,
            oppdatert = EndringType.VILKÅR_VURDERING_OPPDATERT,
            slettet = EndringType.VILKÅR_VURDERING_SLETTET,
        ),
    ) {
    private val vilkårType = VilkårType.NØDVENDIGE_EKSTRAUTGIFTER

    override val periodebeskrivelse = "vilkårsvurdering for ${vilkårType.name}"

    /** Bare perioder vurdert til ja kopieres. */
    override fun skalKopieres(periode: VilkårEkstrautgifter) = periode.erVilkårOppfylt()

    override fun endringsdetalj(periode: VilkårEkstrautgifter) =
        periode.sats?.let { "${vilkårType.name}, Sats: ${it.nummer}" }
            ?: "${vilkårType.name}, Vurdering: ${periode.vurdering.name}"

    override fun nyPeriode(
        behandlingId: UUID,
        request: VilkårEkstrautgifterRequest,
    ) = VilkårEkstrautgifter(behandlingId = behandlingId, vurdering = request.vurdering).oppdaterMed(request)

    override fun VilkårEkstrautgifter.oppdaterMed(request: VilkårEkstrautgifterRequest) =
        copy(
            vurdering = request.vurdering,
            sats = request.sats,
            begrunnelse = request.begrunnelse,
            fraOgMedDato = request.fraOgMedDato,
            tilOgMedDato = request.tilOgMedDato,
        ).medEkstrautgifter(request.ekstrautgifter)

    override fun valider(
        request: VilkårEkstrautgifterRequest,
        behandlingId: UUID,
        eksisterendePeriode: VilkårEkstrautgifter?,
    ) = EkstrautgifterRegler.valider(
        request = request,
        lagret = eksisterendePeriode,
        diagnosePerioder = vilkårDiagnoseService.hentPerioder(behandlingId),
    )
}
