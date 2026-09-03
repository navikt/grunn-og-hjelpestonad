package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.behandling.BehandlingRepository
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class GjeldendeVedtakService(
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
) {
    fun hentGjeldendeVedtakFraDato(
        behandlingId: UUID,
        fra: YearMonth,
    ): HistoriskVedtakResponse {
        val behandling =
            behandlingRepository.findByIdOrNull(behandlingId)
                ?: throw Feil("Fant ikke behandling med id=$behandlingId")

        val alleFerdigstilteBehandlinger =
            behandlingRepository
                .finnAlleIverksatteBehandlinger(behandling.fagsakId)
                .sortedBy { it.sporbar.endret.endretTid }

        val vedtakListe =
            alleFerdigstilteBehandlinger
                .mapNotNull { vedtakRepository.findByBehandlingId(it.id) }

        val fraErFørTidligsteVedtak =
            vedtakListe.flatMap { it.barnetilsynperioder }.all { it.datoFra > fra }

        val sammenslåttPerioder = sammenslåBarnetilsynsperioder(vedtakListe, fra)

        return HistoriskVedtakResponse(
            barnetilsynperioder = sammenslåttPerioder,
            fraErFørTidligsteVedtak = fraErFørTidligsteVedtak,
        )
    }

    private fun sammenslåBarnetilsynsperioder(
        vedtakListe: List<Vedtak>,
        fra: YearMonth,
    ): List<Barnetilsynperiode> {
        val månedTilPeriode = mutableMapOf<YearMonth, MånedPeriodeData>()

        for (vedtak in vedtakListe) {
            if (vedtak.resultatType == ResultatType.OPPHØR && vedtak.opphørFom != null) {
                månedTilPeriode.keys.filter { it >= vedtak.opphørFom }.forEach { month ->
                    månedTilPeriode[month] = MånedPeriodeData.ingenStønad()
                }
            } else if (vedtak.resultatType == ResultatType.INNVILGET) {
                for (periode in vedtak.barnetilsynperioder) {
                    var gjeldendeDato = periode.datoFra
                    while (gjeldendeDato <= periode.datoTil) {
                        månedTilPeriode[gjeldendeDato] =
                            MånedPeriodeData(
                                utgifter = periode.utgifter,
                                barn = periode.barn,
                                periodetype = periode.periodetype,
                                aktivitetstype = periode.aktivitetstype,
                            )
                        gjeldendeDato = gjeldendeDato.plusMonths(1)
                    }
                }
            }
        }

        val filtrerteMåneder = månedTilPeriode.filterKeys { it >= fra }

        return konverterTilBarnetilsynsperioder(filtrerteMåneder)
    }

    private fun konverterTilBarnetilsynsperioder(
        månedTilPeriode: Map<YearMonth, MånedPeriodeData>,
    ): List<Barnetilsynperiode> {
        if (månedTilPeriode.isEmpty()) return emptyList()

        val sorterteMåneder = månedTilPeriode.keys.sorted()
        val resultat = mutableListOf<Barnetilsynperiode>()

        var periodeStart = sorterteMåneder.first()
        var dataStart = månedTilPeriode.getValue(periodeStart)

        for (i in 1 until sorterteMåneder.size) {
            val gjeldendeMåned = sorterteMåneder[i]
            val gjeldendeData = månedTilPeriode.getValue(gjeldendeMåned)

            val forrigeMåned = sorterteMåneder[i - 1]

            if (!kanSlåsSammen(forrigeMåned, gjeldendeMåned, dataStart, gjeldendeData)) {
                resultat.add(dataStart.tilBarnetilsynPeriode(periodeStart, forrigeMåned))

                resultat.addAll(lagIngenStønadPeriodeHvisTomPeriode(forrigeMåned, gjeldendeMåned))

                periodeStart = gjeldendeMåned
                dataStart = gjeldendeData
            }
        }
        resultat.add(dataStart.tilBarnetilsynPeriode(periodeStart, sorterteMåneder.last()))

        return resultat
    }

    private fun kanSlåsSammen(
        forrigeMåned: YearMonth,
        gjeldendeMåned: YearMonth,
        forrigeData: MånedPeriodeData,
        gjeldendeData: MånedPeriodeData,
    ): Boolean {
        val erSammenhengende = forrigeMåned.plusMonths(1) == gjeldendeMåned
        val harLikData = gjeldendeData.erLik(forrigeData)
        return erSammenhengende && harLikData
    }

    private fun lagIngenStønadPeriodeHvisTomPeriode(
        forrigeMåned: YearMonth,
        gjeldendeMåned: YearMonth,
    ): List<Barnetilsynperiode> {
        val ingenPeriodeStart = forrigeMåned.plusMonths(1)
        val ingenPeriodeSlutt = gjeldendeMåned.minusMonths(1)
        return if (ingenPeriodeStart <= ingenPeriodeSlutt) {
            listOf(MånedPeriodeData.ingenStønad().tilBarnetilsynPeriode(ingenPeriodeStart, ingenPeriodeSlutt))
        } else {
            emptyList()
        }
    }
}

private data class MånedPeriodeData(
    val utgifter: BigDecimal,
    val barn: List<UUID>,
    val periodetype: PeriodetypeBarnetilsyn,
    val aktivitetstype: AktivitetstypeBarnetilsyn,
) {
    fun erLik(annenData: MånedPeriodeData): Boolean =
        utgifter == annenData.utgifter &&
            barn.toSet() == annenData.barn.toSet() &&
            periodetype == annenData.periodetype &&
            aktivitetstype == annenData.aktivitetstype

    companion object {
        fun ingenStønad() =
            MånedPeriodeData(
                utgifter = BigDecimal.ZERO,
                barn = emptyList(),
                periodetype = PeriodetypeBarnetilsyn.INGEN_STØNAD,
                aktivitetstype = AktivitetstypeBarnetilsyn.IKKE_RELEVANT,
            )
    }

    fun tilBarnetilsynPeriode(
        datoFra: YearMonth,
        datoTil: YearMonth,
    ) = Barnetilsynperiode(
        datoFra = datoFra,
        datoTil = datoTil,
        utgifter = utgifter,
        barn = barn,
        periodetype = periodetype,
        aktivitetstype = aktivitetstype,
    )
}
