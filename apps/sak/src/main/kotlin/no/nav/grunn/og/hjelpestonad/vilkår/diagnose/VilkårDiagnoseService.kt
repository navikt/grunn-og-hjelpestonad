package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårDiagnoseService(
    vilkårDiagnoseRepository: VilkårDiagnoseRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : VilkårPeriodeService<VilkårDiagnose, VilkårDiagnoseRequest>(
        vilkårDiagnoseRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
    ) {
    override val vilkårType = VilkårType.DIAGNOSE

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

    override fun valider(request: VilkårDiagnoseRequest) = DiagnoseRegler.valider(request)

    override fun validerTidslinje(perioder: List<VilkårDiagnose>) = DiagnoseRegler.validerMaksEnYrkesskadePeriode(perioder)

    /** Kalles etter [valider], så koden finnes. */
    private fun VilkårDiagnoseRequest.diagnosekode() = checkNotNull(Icd10.finn(kode))
}
