package no.nav.grunn.og.hjelpestonad.vedtak

import jdk.jshell.spi.ExecutionControl
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.vedtak.BeregningUtils.beregnBarnetilsynperiode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.any
import kotlin.text.isNullOrEmpty

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val endringshistorikkService: EndringshistorikkService,
    private val behandlingStegService: BehandlingStegService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun hentVedtak(behandlingId: UUID): Vedtak? = vedtakRepository.findByBehandlingId(behandlingId)

    fun hentVedtakEllerKast(behandlingId: UUID): Vedtak = hentVedtak(behandlingId) ?: throw RuntimeException("Behandling=$behandlingId mangler vedtak")

    /**
     * Utleder innvilgelse og avslag fra gjeldende grunnlag, også ved ny sending etter underkjenning.
     * Se ADR-0020 for avgrensningen til førstegangsbehandling og utsatt tilpasning av historikk.
     */
    @Transactional
    fun lagreVedtakFraBehandlingsgrunnlag(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        val eksisterendeVedtak = hentVedtak(behandlingId)
        when (eksisterendeVedtak?.resultatType) {
            ResultatType.OPPHØR, ResultatType.HENLAGT -> throw NotImplementedError()
            ResultatType.INNVILGET, ResultatType.AVSLÅTT, null -> Unit
        }

        val tilkjentYtelse = tilkjentYtelseService.hentTilkjentYtelse(behandlingId)
        val resultatType =
            utledVedtaksresultat(
                harBeregnedeAndeler = tilkjentYtelse?.andelerTilkjentYtelse?.isNotEmpty() == true,
            )
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        if (eksisterendeVedtak != null) {
            vedtakRepository.update(eksisterendeVedtak.copy(resultatType = resultatType, saksbehandlerIdent = saksbehandlerIdent))
        } else {
            vedtakRepository.insert(
                Vedtak(
                    behandlingId = behandlingId,
                    resultatType = resultatType,
                    // Feltet tilhører den gamle barnetilsynsmodellen. Stønadsperiodene ligger i tilkjent ytelse.
                    grunnstønadPerioder = emptyList(),
                    saksbehandlerIdent = saksbehandlerIdent,
                ),
            )
        }
        endringshistorikkService.registrerEndring(behandlingId, EndringType.VEDTAK_LAGRET, detaljer = "Resultat: $resultatType")
    }

    fun validerVedtaksresultatMotBeregning(behandlingId: UUID) {
        val vedtak = hentVedtakEllerKast(behandlingId)
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

    fun lagBeløpsperioder(barnetilsynBeregningRequest: BarnetilsynBeregningRequest): List<BeløpsperioderResponse> = beregnBarnetilsynperiode(barnetilsynBeregningRequest.barnetilsynBeregning)

    fun validerKanLagreVedtak(
        vedtakRequest: VedtakRequest,
    ) {
        if (vedtakRequest.resultatType == ResultatType.INNVILGET) {
            val barnetilsynperioder = vedtakRequest.grunnstønadPerioder

            val månedsPerioder = barnetilsynperioder.map { periode -> Månedsperiode(periode.datoFra, periode.datoTil) }
            validerGyldigePerioder(månedsPerioder)

            val utgifter = barnetilsynperioder.map { periode -> periode.utgifter }
            validerFornuftigeBeløp(utgifter)

            validerAntallBarnAktivitetstypeOgUtgifter(barnetilsynperioder)
            validerOpphørIkkeFørsteEllerSistePeriode(barnetilsynperioder)
        }
        if (vedtakRequest.resultatType == ResultatType.OPPHØR) {
            if (vedtakRequest.opphørFom == null) {
                throw Feil("Kan ikke opphøre uten å velge opphørsdato")
            }
            if (vedtakRequest.grunnstønadPerioder.isNotEmpty()) {
                throw Feil("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
            }
        }
        if (vedtakRequest.resultatType == ResultatType.AVSLÅTT) {
            if (vedtakRequest.grunnstønadPerioder.isNotEmpty()) {
                throw Feil("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
            }
        }
        validerBegrunnelse(vedtakRequest)
    }

    fun validerKanBeregne(
        barnetilsynBeregningRequest: BarnetilsynBeregningRequest,
    ) {
        val barnetilsynBeregninger = barnetilsynBeregningRequest.barnetilsynBeregning

        val månedsperioder = barnetilsynBeregninger.map { periode -> Månedsperiode(periode.datoFra, periode.datoTil) }
        validerGyldigePerioder(månedsperioder)

        val utgifter = barnetilsynBeregninger.map { periode -> periode.utgifter }
        validerFornuftigeBeløp(utgifter)
    }

    private fun validerGyldigePerioder(
        månedsperioder: List<Månedsperiode>,
    ) {
        if (månedsperioder.isEmpty()) {
            throw Feil("Ingen perioder")
        }
        if (månedsperioder.harOverlappende()) {
            throw Feil("Har overlappende perioder")
        }
        if (!månedsperioder.erSammenhengende()) {
            throw Feil("Perioder må være sammenhengende")
        }
    }

    private fun validerFornuftigeBeløp(
        utgifter: List<BigDecimal>,
    ) {
        if (utgifter.any({ it.toInt() < 0 })) {
            throw Feil("Utgifter kan ikke være mindre enn 0")
        }
        if (utgifter.any({ it.toInt() > 40000 })) {
            throw Feil("Utgifter på mer enn 40000 støttes ikke")
        }
    }

    private fun validerAntallBarnAktivitetstypeOgUtgifter(
        barnetilsynperioder: List<GrunnstønadPeriode>,
    ) {
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.barn.isNotEmpty() }) {
            throw Feil("Kan ikke ta med barn på en periode som er av type ingen stønad")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.utgifter.toInt() > 0 }) {
            throw Feil("Kan ikke ha utgifter større enn null på en periode som er av type ingen stønad")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.barn.isEmpty() }) {
            throw Feil("Må ha med minst et barn på en periode som er ordinær")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.utgifter.toInt() <= 0 }) {
            throw Feil("Kan ikke ha null utgifter på en periode som er ordinær")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.aktivitetstype != AktivitetstypeBarnetilsyn.IKKE_RELEVANT }) {
            throw Feil("Kan ikke ha aktivitetstype på ingen stønad periode")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.aktivitetstype == AktivitetstypeBarnetilsyn.IKKE_RELEVANT }) {
            throw Feil("Kan ikke ha tom aktivitetstype for ordinær periode")
        }
    }

    private fun validerOpphørIkkeFørsteEllerSistePeriode(
        barnetilsynperioder: List<GrunnstønadPeriode>,
    ) {
        if (barnetilsynperioder.first().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw Feil("Første periode kan ikke være periodetype ingen stønad")
        }
        if (barnetilsynperioder.last().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw Feil("Siste periode kan ikke være periodetype ingen stønad")
        }
    }

    private fun validerBegrunnelse(
        vedtakRequest: VedtakRequest,
    ) {
        if (vedtakRequest.begrunnelse.isNullOrEmpty()) {
            throw Feil("Mangler begrunnelse")
        }
    }
}
