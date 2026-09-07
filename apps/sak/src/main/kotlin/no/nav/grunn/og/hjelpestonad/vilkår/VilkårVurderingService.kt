package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårVurderingService(
    private val vilkårVurderingRepository: VilkårVurderingRepository,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun hentVilkårVurderinger(behandlingId: UUID): List<VilkårVurdering> = vilkårVurderingRepository.findByBehandlingId(behandlingId)

    fun lagreVilkårVurdering(
        behandlingId: UUID,
        request: VilkårVurderingRequest,
    ): VilkårVurdering {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val eksisterendeVurdering =
            vilkårVurderingRepository.findByBehandlingIdAndVilkårType(
                behandlingId = behandlingId,
                vilkårType = request.vilkårType,
            )

        if (eksisterendeVurdering == null) {
            val vurdering =
                vilkårVurderingRepository.insert(
                    VilkårVurdering(
                        behandlingId = behandlingId,
                        vilkårType = request.vilkårType,
                        vurdering = request.vurdering,
                        begrunnelse = request.begrunnelse,
                    ),
                )
            endringshistorikkService.registrerEndring(
                behandlingId = behandlingId,
                endringType = EndringType.VILKÅR_VURDERING_OPPRETTET,
                detaljer = "${request.vilkårType}: ${request.vurdering}",
            )
            return vurdering
        }

        val oppdatert =
            vilkårVurderingRepository.update(
                eksisterendeVurdering.copy(
                    vurdering = request.vurdering,
                    begrunnelse = request.begrunnelse,
                ),
            )
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.VILKÅR_VURDERING_OPPDATERT,
            detaljer = "${request.vilkårType}: ${request.vurdering}",
        )
        return oppdatert
    }
}
