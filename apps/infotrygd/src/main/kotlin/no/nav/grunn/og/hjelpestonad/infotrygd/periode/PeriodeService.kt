package no.nav.grunn.og.hjelpestonad.infotrygd.periode

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.EvalueringService
import no.nav.grunn.og.hjelpestonad.infotrygd.stønad.Stønadstype
import org.springframework.stereotype.Service

@Service
class PeriodeService(
    private val periodeRepository: PeriodeRepository,
    private val evalueringService: EvalueringService,
) {
    fun hentPerioder(request: PeriodeRequest): Map<Stønadstype, List<PeriodeResponse>> {
        val perioder =
            periodeRepository
                .hentPerioder(request.personidenter, request.stønadstyper.ifEmpty { Stønadstype.entries.toSet() })
                .filter(::harOppdragIdEller0beløp)
        val vedtakIder = perioder.map { it.vedtakId }.toSet()
        val trygdetidOgSats = periodeRepository.hentTrygdetidOgSats(vedtakIder)
        val evalueringer = evalueringService.hentEvalueringerForVedtak(vedtakIder)
        return perioder
            .map {
                it.copy(
                    trygdetidOgSats = trygdetidOgSats[it.vedtakId].orEmpty(),
                    evaluering = evalueringer[it.vedtakId],
                )
            }.groupBy { it.stønadstype }
            .mapValues { (_, perioder) -> perioder.sortedByDescending { it.stønadFom } }
    }

    /**
     * Perioder uten oppdragId og med beløp over 0 kr er ikke iverksatt, og filtreres bort. Perioder uten
     * oppdragId og med 0 kr er besluttet, men ikke sendt til oppdrag. Samme regel som i familie-ef-infotrygd-replika.
     */
    private fun harOppdragIdEller0beløp(periode: PeriodeResponse) = periode.oppdragId != null || periode.beløp.signum() == 0
}
