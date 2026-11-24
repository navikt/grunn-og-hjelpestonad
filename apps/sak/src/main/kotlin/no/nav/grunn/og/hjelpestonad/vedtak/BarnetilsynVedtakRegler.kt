package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import java.math.BigDecimal

object BarnetilsynVedtakRegler {
    fun validerKanLagreVedtak(vedtakRequest: VedtakRequest) {
        when (vedtakRequest.resultatType) {
            ResultatType.INNVILGET -> {
                val barnetilsynperioder = vedtakRequest.grunnstønadPerioder
                val månedsperioder = barnetilsynperioder.map { periode -> Månedsperiode(periode.datoFra, periode.datoTil) }
                validerGyldigePerioder(månedsperioder)
                validerFornuftigeBeløp(barnetilsynperioder.map { it.utgifter })
                validerAntallBarnAktivitetstypeOgUtgifter(barnetilsynperioder)
                validerOpphørIkkeFørsteEllerSistePeriode(barnetilsynperioder)
            }

            ResultatType.OPPHØR -> {
                if (vedtakRequest.opphørFom == null) {
                    throw UgyldigInput("Kan ikke opphøre uten å velge opphørsdato")
                }
                if (vedtakRequest.grunnstønadPerioder.isNotEmpty()) {
                    throw UgyldigInput("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
                }
            }

            ResultatType.AVSLÅTT -> {
                if (vedtakRequest.grunnstønadPerioder.isNotEmpty()) {
                    throw UgyldigInput("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
                }
            }

            ResultatType.HENLAGT -> {}
        }
        if (vedtakRequest.begrunnelse.isNullOrEmpty()) {
            throw UgyldigInput("Mangler begrunnelse")
        }
    }

    fun validerKanBeregne(barnetilsynBeregningRequest: BarnetilsynBeregningRequest) {
        val barnetilsynBeregninger = barnetilsynBeregningRequest.barnetilsynBeregning
        val månedsperioder = barnetilsynBeregninger.map { periode -> Månedsperiode(periode.datoFra, periode.datoTil) }
        validerGyldigePerioder(månedsperioder)
        validerFornuftigeBeløp(barnetilsynBeregninger.map { it.utgifter })
    }

    private fun validerGyldigePerioder(månedsperioder: List<Månedsperiode>) {
        if (månedsperioder.isEmpty()) {
            throw UgyldigInput("Ingen perioder")
        }
        if (månedsperioder.harOverlappende()) {
            throw UgyldigInput("Har overlappende perioder")
        }
        if (!månedsperioder.erSammenhengende()) {
            throw UgyldigInput("Perioder må være sammenhengende")
        }
    }

    private fun validerFornuftigeBeløp(utgifter: List<BigDecimal>) {
        if (utgifter.any { it.toInt() < 0 }) {
            throw UgyldigInput("Utgifter kan ikke være mindre enn 0")
        }
        if (utgifter.any { it.toInt() > 40000 }) {
            throw UgyldigInput("Utgifter på mer enn 40000 støttes ikke")
        }
    }

    private fun validerAntallBarnAktivitetstypeOgUtgifter(barnetilsynperioder: List<GrunnstønadPeriode>) {
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.barn.isNotEmpty() }) {
            throw UgyldigInput("Kan ikke ta med barn på en periode som er av type ingen stønad")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.utgifter.toInt() > 0 }) {
            throw UgyldigInput("Kan ikke ha utgifter større enn null på en periode som er av type ingen stønad")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.barn.isEmpty() }) {
            throw UgyldigInput("Må ha med minst et barn på en periode som er ordinær")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.utgifter.toInt() <= 0 }) {
            throw UgyldigInput("Kan ikke ha null utgifter på en periode som er ordinær")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD && it.aktivitetstype != AktivitetstypeBarnetilsyn.IKKE_RELEVANT }) {
            throw UgyldigInput("Kan ikke ha aktivitetstype på ingen stønad periode")
        }
        if (barnetilsynperioder.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.aktivitetstype == AktivitetstypeBarnetilsyn.IKKE_RELEVANT }) {
            throw UgyldigInput("Kan ikke ha tom aktivitetstype for ordinær periode")
        }
    }

    private fun validerOpphørIkkeFørsteEllerSistePeriode(barnetilsynperioder: List<GrunnstønadPeriode>) {
        if (barnetilsynperioder.first().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw UgyldigInput("Første periode kan ikke være periodetype ingen stønad")
        }
        if (barnetilsynperioder.last().periodetype == PeriodetypeBarnetilsyn.INGEN_STØNAD) {
            throw UgyldigInput("Siste periode kan ikke være periodetype ingen stønad")
        }
    }
}
