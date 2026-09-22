package no.nav.grunn.og.hjelpestonad.vilkår.diagnose

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import org.springframework.http.HttpStatus
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
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        diagnose = request.normalisertDiagnose(),
        erYrkesskade = request.erYrkesskade,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    override fun oppdatertPeriode(
        eksisterende: VilkårDiagnose,
        request: VilkårDiagnoseRequest,
    ) = eksisterende.copy(
        diagnose = request.normalisertDiagnose(),
        erYrkesskade = request.erYrkesskade,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    /**
     * Et medlem kan ha flere diagnoser samtidig, så perioder for ulike diagnoser får overlappe.
     * Samme diagnose kan derimot ikke vurderes to ganger for samme tidsrom.
     */
    override fun perioderSomIkkeKanOverlappe(
        lagredePerioder: List<VilkårDiagnose>,
        request: VilkårDiagnoseRequest,
    ) = lagredePerioder.filter { it.diagnose.equals(request.normalisertDiagnose(), ignoreCase = true) }

    override fun valider(request: VilkårDiagnoseRequest) {
        if (request.diagnose.isBlank()) {
            throw Feil(melding = "Diagnose kan ikke være tom", httpStatus = HttpStatus.BAD_REQUEST)
        }
        // Skadedatoen avgjør om § 6-9 lemper medlemskapskravet, jf. ADR-0004.
        if (request.erYrkesskade && request.fraOgMedDato == null) {
            throw Feil(melding = "En yrkesskade må ha en fra og med-dato", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun VilkårDiagnoseRequest.normalisertDiagnose() = diagnose.trim()
}
