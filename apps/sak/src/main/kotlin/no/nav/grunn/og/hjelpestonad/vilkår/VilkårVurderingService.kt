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

@Service
class VilkårVurderingService(
    private val vilkårVurderingRepository: VilkårVurderingRepository,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun hentVilkårVurderinger(behandlingId: UUID): List<VilkårVurdering> = vilkårVurderingRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun lagreVilkårVurdering(
        behandlingId: UUID,
        request: VilkårVurderingRequest,
    ): VilkårVurdering {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        validerPeriode(request)
        validerIngenOverlappendePerioder(behandlingId, request)

        return if (request.id == null) {
            opprettPeriode(behandlingId, request)
        } else {
            oppdaterPeriode(behandlingId, request.id, request)
        }
    }

    @Transactional
    fun slettVilkårVurdering(
        behandlingId: UUID,
        vurderingId: UUID,
    ) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val vurdering = hentVurderingPåBehandling(behandlingId, vurderingId)

        vilkårVurderingRepository.deleteById(vurdering.id)

        registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_SLETTET, vurdering.vilkårType, vurdering.vurdering)
    }

    private fun opprettPeriode(
        behandlingId: UUID,
        request: VilkårVurderingRequest,
    ): VilkårVurdering {
        val vurdering =
            vilkårVurderingRepository.insert(
                VilkårVurdering(
                    behandlingId = behandlingId,
                    vilkårType = request.vilkårType,
                    vurdering = request.vurdering,
                    begrunnelse = request.begrunnelse,
                    fraOgMedDato = request.fraOgMedDato,
                    tilOgMedDato = request.tilOgMedDato,
                ),
            )
        registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPRETTET, request.vilkårType, request.vurdering)
        return vurdering
    }

    private fun oppdaterPeriode(
        behandlingId: UUID,
        vurderingId: UUID,
        request: VilkårVurderingRequest,
    ): VilkårVurdering {
        val eksisterende = hentVurderingPåBehandling(behandlingId, vurderingId)
        if (eksisterende.vilkårType != request.vilkårType) {
            throw Feil(
                melding = "Kan ikke endre vilkårstype på en eksisterende periode. Slett perioden og opprett en ny.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        val oppdatert =
            vilkårVurderingRepository.update(
                eksisterende.copy(
                    vurdering = request.vurdering,
                    begrunnelse = request.begrunnelse,
                    fraOgMedDato = request.fraOgMedDato,
                    tilOgMedDato = request.tilOgMedDato,
                ),
            )
        registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPDATERT, request.vilkårType, request.vurdering)
        return oppdatert
    }

    fun hentVurderingPåBehandling(
        behandlingId: UUID,
        vurderingId: UUID,
    ): VilkårVurdering =
        vilkårVurderingRepository
            .findByIdOrNull(vurderingId)
            ?.takeIf { it.behandlingId == behandlingId }
            ?: throw Feil(
                melding = "Fant ikke vilkårsvurdering med id=$vurderingId på behandling=$behandlingId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

    private fun validerPeriode(request: VilkårVurderingRequest) {
        val fraOgMedDato = request.fraOgMedDato
        val tilOgMedDato = request.tilOgMedDato
        if (fraOgMedDato != null && tilOgMedDato != null && tilOgMedDato < fraOgMedDato) {
            throw Feil(
                melding = "Til og med-dato $tilOgMedDato kan ikke være før fra og med-dato $fraOgMedDato",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    // Todo: Kan håndteres av tidslinjerammeverk.
    private fun validerIngenOverlappendePerioder(
        behandlingId: UUID,
        request: VilkårVurderingRequest,
    ) {
        val overlapper =
            vilkårVurderingRepository
                .findByBehandlingIdAndVilkårType(behandlingId = behandlingId, vilkårType = request.vilkårType)
                .filterNot { it.id == request.id }
                .any { it.overlapper(request.fraOgMedDato, request.tilOgMedDato) }

        if (overlapper) {
            throw Feil(
                melding = "Perioden overlapper en eksisterende periode for vilkåret ${request.vilkårType}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    /**
     * Diagnose er en helseopplysning etter GDPR artikkel 9 og skal aldri registreres i
     * endringshistorikk eller logg. Ikke utvid detaljer med diagnosefelter.
     */
    private fun registrerEndring(
        behandlingId: UUID,
        endringType: EndringType,
        vilkårType: VilkårType,
        vurdering: Vurdering,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = endringType,
        detaljer = "$vilkårType: $vurdering",
    )
}
