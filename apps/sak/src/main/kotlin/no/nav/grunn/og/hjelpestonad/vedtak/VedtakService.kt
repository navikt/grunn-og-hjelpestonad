package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårStegService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val endringshistorikkService: EndringshistorikkService,
    private val behandlingStegService: BehandlingStegService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val behandlingService: BehandlingService,
    private val inngangsvilkårStegService: InngangsvilkårStegService,
) {
    fun hentVedtak(behandlingId: UUID): Vedtak? = vedtakRepository.findByBehandlingId(behandlingId)

    /**
     * Utleder innvilgelse og avslag fra gjeldende grunnlag, også ved ny sending etter underkjenning.
     * Se ADR-0019 for avgrensningen til førstegangsbehandling og utsatt tilpasning av historikk.
     */
    @Transactional
    fun lagreVedtakFraBehandlingsgrunnlag(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        val eksisterendeVedtak = hentVedtak(behandlingId)
        when (eksisterendeVedtak?.resultatType) {
            ResultatType.OPPHØR, ResultatType.HENLAGT -> return
            ResultatType.INNVILGET, ResultatType.AVSLÅTT, null -> Unit
        }

        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)
        val resultatType =
            utledVedtaksresultat(
                harOppfylteInngangsvilkår = inngangsvilkårStegService.hentPerioder(behandlingId).isNotEmpty(),
                harBeregnedeAndeler = tilkjentYtelse?.andelerTilkjentYtelse?.isNotEmpty() == true,
                erRevurdering = behandling.forrigeBehandlingId != null,
            )
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        if (eksisterendeVedtak != null) {
            vedtakRepository.update(eksisterendeVedtak.copy(resultatType = resultatType, saksbehandlerIdent = saksbehandlerIdent))
        } else {
            vedtakRepository.insert(
                Vedtak(
                    behandlingId = behandlingId,
                    resultatType = resultatType,
                    grunnstønadPerioder = emptyList(),
                    saksbehandlerIdent = saksbehandlerIdent,
                ),
            )
        }
        endringshistorikkService.registrerEndring(behandlingId, EndringType.VEDTAK_LAGRET, detaljer = "Resultat: $resultatType")
    }

    fun validerVedtaksresultatMotBeregning(behandlingId: UUID) {
        val vedtak = hentVedtak(behandlingId) ?: throw Feil("Lagre vedtaket før du sender behandlingen til beslutter", HttpStatus.BAD_REQUEST)
        when (vedtak.resultatType) {
            ResultatType.INNVILGET, ResultatType.AVSLÅTT -> {}

            ResultatType.OPPHØR, ResultatType.HENLAGT -> {
                return
            }
        }

        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)
        if (vedtak.resultatType == ResultatType.INNVILGET && tilkjentYtelse == null) {
            throw Feil("Fullfør vilkår og sats og trygdetid på nytt før du sender behandlingen til beslutter", HttpStatus.BAD_REQUEST)
        }
        validerVedtaksresultatMotBeregning(vedtak.resultatType, tilkjentYtelse?.andelerTilkjentYtelse?.isNotEmpty() == true)
    }

    @Transactional
    fun lagreVedtak(
        vedtakRequest: VedtakRequest,
        behandlingId: UUID,
    ): UUID {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        when (vedtakRequest.resultatType) {
            ResultatType.INNVILGET, ResultatType.OPPHØR -> {
                if (tilkjentYtelseService.hentTilkjentYtelse(behandlingId) == null) {
                    throw Feil("Fullfør sats og trygdetid før du lagrer vedtaket")
                }
            }

            ResultatType.AVSLÅTT, ResultatType.HENLAGT -> {}
        }
        if (vedtakRepository.findByBehandlingId(behandlingId) != null) {
            vedtakRepository.deleteByBehandlingId(behandlingId)
        }
        val vedtak = vedtakRepository.insert(vedtakRequest.tilVedtak(behandlingId))

        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.VEDTAK_LAGRET,
            detaljer = "Resultat: ${vedtakRequest.resultatType}",
        )
        return vedtak.behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        if (vedtakRepository.findByBehandlingId(behandlingId) != null) {
            vedtakRepository.deleteByBehandlingId(behandlingId)
        }
    }
}
