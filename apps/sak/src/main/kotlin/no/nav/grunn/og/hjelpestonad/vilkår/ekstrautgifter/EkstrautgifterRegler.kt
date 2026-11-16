package no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.erOppfyltTidslinje

object EkstrautgifterRegler {
    const val FRA_OG_MED_MANGLER = "Ekstrautgiftene må ha en fra og med-dato når vilkåret er oppfylt."
    const val SATS_MANGLER = "Velg sats når vilkåret er oppfylt."
    const val SATS_UTEN_RETT = "En periode som ikke er oppfylt, kan ikke ha sats."
    const val EKSTRAUTGIFT_MANGLER = "Legg til minst én ekstrautgift når vilkåret er oppfylt."
    const val DIAGNOSE_MANGLER = "Velg minst én diagnose for hver ekstrautgift."
    const val HJEMMELBOKSTAV_FLERE_GANGER = "En periode kan bare ha én ekstrautgift per hjemmelbokstav."
    const val BOKSTAV_GJELDER_IKKE_NYE_TILFELLER =
        "Bokstav d og e gir ikke grunnstønad i nye tilfeller fra 1. januar 2020. " +
            "De kan bare beholdes i en periode som allerede har dem."
    const val DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN =
        "Minst én av de valgte diagnosene er ikke vurdert til oppfylt i hele perioden. " +
            "Endre perioden, eller vurder diagnosen i steg 1."

    fun valider(
        request: VilkårEkstrautgifterRequest,
        lagret: VilkårEkstrautgifter?,
        diagnosePerioder: List<VilkårDiagnose>,
    ) {
        validerHverEkstrautgiftHarDiagnose(request.ekstrautgifter)
        validerKunEnEkstrautgiftPerBokstav(request.ekstrautgifter)
        if (request.vurdering == Vurdering.JA) validerOppfylt(request) else validerIkkeOppfylt(request.sats)
        validerBokstaverForNyeTilfeller(request.ekstrautgifter, lagret)
        validerDiagnoserOppfyltIHelePerioden(request, diagnosePerioder)
    }

    /**
     * Må stå før [validerDiagnoserOppfyltIHelePerioden]. Den regelen godtar en ekstrautgift uten diagnoser, fordi alle
     * diagnosene i et tomt sett regnes som oppfylt.
     */
    private fun validerHverEkstrautgiftHarDiagnose(ekstrautgifter: List<Ekstrautgift>) {
        if (ekstrautgifter.any { it.diagnosekoder.isEmpty() }) {
            throw UgyldigInput(DIAGNOSE_MANGLER)
        }
    }

    private fun validerKunEnEkstrautgiftPerBokstav(ekstrautgifter: List<Ekstrautgift>) {
        val bokstaver = ekstrautgifter.map { it.hjemmelbokstav }
        if (bokstaver.size != bokstaver.toSet().size) {
            throw UgyldigInput(HJEMMELBOKSTAV_FLERE_GANGER)
        }
    }

    private fun validerOppfylt(request: VilkårEkstrautgifterRequest) {
        if (request.fraOgMedDato == null) {
            throw UgyldigInput(FRA_OG_MED_MANGLER)
        }
        if (request.sats == null) {
            throw UgyldigInput(SATS_MANGLER)
        }
        if (request.ekstrautgifter.isEmpty()) {
            throw UgyldigInput(EKSTRAUTGIFT_MANGLER)
        }
    }

    private fun validerIkkeOppfylt(sats: Sats?) {
        if (sats != null) {
            throw UgyldigInput(SATS_UTEN_RETT)
        }
    }

    /** Bokstav d og e kan stå i en periode som allerede har dem, for eksempel fra Infotrygd, men aldri legges til. */
    private fun validerBokstaverForNyeTilfeller(
        ekstrautgifter: List<Ekstrautgift>,
        lagret: VilkårEkstrautgifter?,
    ) {
        val nyeBokstaver = ekstrautgifter.map { it.hjemmelbokstav }.toSet() - lagret?.bokstaver.orEmpty()
        if (nyeBokstaver.any { !it.gjelderNyeTilfeller }) {
            throw UgyldigInput(BOKSTAV_GJELDER_IKKE_NYE_TILFELLER)
        }
    }

    private fun validerDiagnoserOppfyltIHelePerioden(
        request: VilkårEkstrautgifterRequest,
        diagnosePerioder: List<VilkårDiagnose>,
    ) {
        val periodeForEkstrautgift = Periode(true, request.fraOgMedDato, request.tilOgMedDato).tilTidslinje()
        val diagnoserEtterKode = diagnosePerioder.groupBy { it.tidslinjenøkkel() }
        val alleOppfylt =
            request.diagnosekoder.all { kode ->
                periodeForEkstrautgift.dekkesAv(diagnoserEtterKode[kode].orEmpty().erOppfyltTidslinje())
            }
        if (!alleOppfylt) {
            throw UgyldigInput(DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
        }
    }

    private fun Tidslinje<Boolean>.dekkesAv(oppfylt: Tidslinje<Boolean>): Boolean =
        kombinerMed(oppfylt) { erEkstrautgiftIPeriode, erDiagnoseOppfyltIPeriode ->
            erEkstrautgiftIPeriode != true || erDiagnoseOppfyltIPeriode == true
        }.tilPerioder()
            .filtrerIkkeNull()
            .all { it.verdi }
}
