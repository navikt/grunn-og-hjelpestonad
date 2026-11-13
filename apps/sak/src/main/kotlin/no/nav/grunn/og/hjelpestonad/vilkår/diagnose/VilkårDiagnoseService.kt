package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

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
class VilkårDiagnoseService(
    vilkårDiagnoseRepository: VilkårDiagnoseRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : PeriodeMedBegrunnelseService<VilkårDiagnose, VilkårDiagnoseRequest>(
        vilkårDiagnoseRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
        Endringstyper(
            opprettet = EndringType.VILKÅR_VURDERING_OPPRETTET,
            oppdatert = EndringType.VILKÅR_VURDERING_OPPDATERT,
            slettet = EndringType.VILKÅR_VURDERING_SLETTET,
        ),
    ) {
    override val periodebeskrivelse = "vilkårsvurdering for ${VilkårType.DIAGNOSE.name}"

    /** Bare perioder vurdert til ja kopieres. */
    override fun skalKopieres(periode: VilkårDiagnose) = periode.erVilkårOppfylt()

    override fun endringsdetalj(periode: VilkårDiagnose) = "${VilkårType.DIAGNOSE.name}, Vurdering: ${periode.vurdering.name}"

    override fun nyPeriode(
        behandlingId: UUID,
        request: VilkårDiagnoseRequest,
    ) = request.diagnosekode().let { diagnosekode ->
        VilkårDiagnose(
            behandlingId = behandlingId,
            kode = diagnosekode.kode,
            tekst = diagnosekode.tekst,
            erYrkesskade = request.erYrkesskade,
            vurdering = request.vurdering,
            begrunnelse = request.begrunnelse,
            fraOgMedDato = request.fraOgMedDato,
            tilOgMedDato = request.tilOgMedDato,
        )
    }

    override fun VilkårDiagnose.oppdaterMed(
        request: VilkårDiagnoseRequest,
    ) = request.diagnosekode().let { diagnosekode ->
        copy(
            kode = diagnosekode.kode,
            tekst = diagnosekode.tekst,
            erYrkesskade = request.erYrkesskade,
            vurdering = request.vurdering,
            begrunnelse = request.begrunnelse,
            fraOgMedDato = request.fraOgMedDato,
            tilOgMedDato = request.tilOgMedDato,
        )
    }

    override fun valider(
        request: VilkårDiagnoseRequest,
        behandlingId: UUID,
        eksisterendePeriode: VilkårDiagnose?,
    ) = DiagnoseRegler.valider(request)

    override fun validerTidslinje(perioder: List<VilkårDiagnose>) = DiagnoseRegler.validerMaksEnYrkesskadePeriode(perioder)

    /** Kalles etter [valider], så koden finnes. */
    private fun VilkårDiagnoseRequest.diagnosekode() = checkNotNull(Icd10.finn(kode))
}
