package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringType
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.gjenlevende.bs.sak.vedtak.BeregningUtils.beregnBarnetilsynperiode
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID
import kotlin.collections.any
import kotlin.text.isNullOrEmpty

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val endringshistorikkService: EndringshistorikkService,
    private val behandlingService: BehandlingService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun hentVedtak(behandlingId: UUID): Vedtak? = vedtakRepository.findByBehandlingId(behandlingId)

    fun lagreVedtak(
        vedtakDto: VedtakDto,
        behandlingId: UUID,
    ): UUID {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val vedtak = vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId))
        tilkjentYtelseService.opprettEllerOppdaterTilkjentYtelse(behandlingId, vedtak)

        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.VEDTAK_LAGRET,
            detaljer = "Resultat: ${vedtakDto.resultatType}",
        )
        return vedtak.behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        if (vedtakRepository.findByBehandlingId(behandlingId) != null) {
            tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId)
            vedtakRepository.deleteByBehandlingId(behandlingId)
        }
    }

    fun lagBeløpsperioder(barnetilsynBeregningRequest: BarnetilsynBeregningRequest): List<BeløpsperioderDto> = beregnBarnetilsynperiode(barnetilsynBeregningRequest.barnetilsynBeregning)

    fun validerKanLagreVedtak(
        vedtakDto: VedtakDto,
    ) {
        if (vedtakDto.resultatType == ResultatType.INNVILGET) {
            val barnetilsynperioder = vedtakDto.barnetilsynperioder

            val månedsPerioder = barnetilsynperioder.map { periode -> Månedsperiode(periode.datoFra, periode.datoTil) }
            validerGyldigePerioder(månedsPerioder)

            val utgifter = barnetilsynperioder.map { periode -> periode.utgifter }
            validerFornuftigeBeløp(utgifter)

            validerAntallBarnAktivitetstypeOgUtgifter(barnetilsynperioder)
            validerOpphørIkkeFørsteEllerSistePeriode(barnetilsynperioder)
        }
        if (vedtakDto.resultatType == ResultatType.OPPHØR) {
            if (vedtakDto.opphørFom == null) {
                throw Feil("Kan ikke opphøre uten å velge opphørsdato")
            }
            if (vedtakDto.barnetilsynperioder.isNotEmpty()) {
                throw Feil("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
            }
        }
        if (vedtakDto.resultatType == ResultatType.AVSLÅTT) {
            if (vedtakDto.barnetilsynperioder.isNotEmpty()) {
                throw Feil("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
            }
        }
        validerBegrunnelse(vedtakDto)
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
        barnetilsynperioder: List<Barnetilsynperiode>,
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
        barnetilsynperioder: List<Barnetilsynperiode>,
    ) {
        if (barnetilsynperioder.first().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw Feil("Første periode kan ikke være periodetype ingen stønad")
        }
        if (barnetilsynperioder.last().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw Feil("Siste periode kan ikke være periodetype ingen stønad")
        }
    }

    private fun validerBegrunnelse(
        vedtakDto: VedtakDto,
    ) {
        if (vedtakDto.begrunnelse.isNullOrEmpty()) {
            throw Feil("Mangler begrunnelse")
        }
    }
}
