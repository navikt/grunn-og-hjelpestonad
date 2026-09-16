package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Diagnose er helseopplysning etter GDPR artikkel 9 og har derfor egen tjeneste og eget
 * endepunkt, atskilt fra de generiske vilkårsvurderingene, jf. ADR-0003.
 *
 * Verken diagnosekode eller diagnosebeskrivelse skal skrives til endringshistorikk eller
 * logg. At diagnosen henger på riktig vilkårstype håndheves av databasen.
 */
@Service
class VilkårDiagnoseService(
    private val vilkårDiagnoseRepository: VilkårDiagnoseRepository,
    private val vilkårVurderingService: VilkårVurderingService,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun hentDiagnoser(
        behandlingId: UUID,
        vurderingId: UUID,
    ): List<VilkårDiagnose> {
        vilkårVurderingService.hentVurderingPåBehandling(behandlingId, vurderingId)
        return vilkårDiagnoseRepository.findByVilkårVurderingId(vurderingId)
    }

    @Transactional
    fun lagreDiagnose(
        behandlingId: UUID,
        vurderingId: UUID,
        request: DiagnoseRequest,
    ): VilkårDiagnose {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val vurdering = vilkårVurderingService.hentVurderingPåBehandling(behandlingId, vurderingId)
        validerDiagnose(request)

        val lagret =
            if (request.id == null) {
                vilkårDiagnoseRepository.insert(
                    VilkårDiagnose(
                        vilkårVurderingId = vurdering.id,
                        diagnose = request.diagnose,
                        yrkesskade = request.yrkesskade,
                        yrkesskadeDato = request.yrkesskadeDato,
                    ),
                )
            } else {
                vilkårDiagnoseRepository.update(
                    hentDiagnosePåVurdering(vurdering.id, request.id).copy(
                        diagnose = request.diagnose,
                        yrkesskade = request.yrkesskade,
                        yrkesskadeDato = request.yrkesskadeDato,
                    ),
                )
            }

        registrerEndring(behandlingId, vurdering)
        return lagret
    }

    @Transactional
    fun slettDiagnose(
        behandlingId: UUID,
        vurderingId: UUID,
        diagnoseId: UUID,
    ) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val vurdering = vilkårVurderingService.hentVurderingPåBehandling(behandlingId, vurderingId)
        val diagnose = hentDiagnosePåVurdering(vurdering.id, diagnoseId)

        vilkårDiagnoseRepository.deleteById(diagnose.id)

        registrerEndring(behandlingId, vurdering)
    }

    /**
     * Hindrer at en diagnose som hører til en annen vilkårsperiode kan leses, endres eller
     * slettes gjennom denne perioden.
     */
    private fun hentDiagnosePåVurdering(
        vurderingId: UUID,
        diagnoseId: UUID,
    ): VilkårDiagnose =
        vilkårDiagnoseRepository
            .findByIdOrNull(diagnoseId)
            ?.takeIf { it.vilkårVurderingId == vurderingId }
            ?: throw Feil(
                melding = "Fant ikke diagnose med id=$diagnoseId på vilkårsvurdering=$vurderingId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

    private fun validerDiagnose(request: DiagnoseRequest) {
        if (request.diagnose.isBlank()) {
            throw Feil(melding = "Diagnose kan ikke være tom", httpStatus = HttpStatus.BAD_REQUEST)
        }
        if (request.yrkesskadeDato != null && !request.yrkesskade) {
            throw Feil(
                melding = "Yrkesskadedato kan ikke settes uten at diagnosen er merket som yrkesskade",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    /**
     * Registrerer at vilkårsvurderingen er endret, uten å røpe noe om diagnosen.
     * Ikke utvid detaljer med diagnosekode eller diagnosebeskrivelse.
     */
    private fun registrerEndring(
        behandlingId: UUID,
        vurdering: VilkårVurdering,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = EndringType.VILKÅR_VURDERING_OPPDATERT,
        detaljer = "${vurdering.vilkårType}: ${vurdering.vurdering}",
    )
}
