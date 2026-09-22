package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.erOppfyltTidslinje
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.tomTidslinje
import java.time.LocalDate

fun List<VilkårMedlemskap>.erOppfyltTidslinje(diagnoser: List<VilkårDiagnose>): Tidslinje<Boolean> {
    val medlemskapTidslinje = erOppfyltTidslinje()

    return diagnoser
        .filter { it.erVilkårOppfylt() }
        .groupBy { it.diagnose.trim().lowercase() }
        .values
        .map { perioderMedSammeDiagnose -> perioderMedSammeDiagnose.medMedlemskapskrav(medlemskapTidslinje) }
        .reduceOrNull { venstreTidslinje, høyreTidslinje -> venstreTidslinje.eller(høyreTidslinje) }
        ?: tomTidslinje()
}

/** Periodene gjelder den samme diagnose. */
private fun List<VilkårDiagnose>.medMedlemskapskrav(medlemskapTidslinje: Tidslinje<Boolean>): Tidslinje<Boolean> {
    val diagnoseTidslinje = erOppfyltTidslinje()

    if (any { it.erYrkesskade }) {
        // Forenkling: en yrkesskade som er delt opp — av et opphold, en periode vurdert til NEI
        // eller bare en ny vurdering — gir flere mulige skadedatoer å måle medlemskapet på. Hvilken
        // som gjelder er ikke avklart med fag, så vi stopper i stedet for å gjette. Meldingene
        // nevner ikke diagnosen, som er helseopplysning etter GDPR artikkel 9.
        require(size == 1) { "En yrkesskade fordelt på flere perioder støttes ikke enda." }

        val skadedato = requireNotNull(single().fraOgMedDato) { "En yrkesskade må ha en fra og med-dato." }

        // § 6-9 lemper medlemskapskravet ved yrkesskade: var personen medlem da skaden oppsto,
        // er kravet oppfylt ut perioden selv om medlemskapet faller bort underveis.
        if (medlemskapTidslinje.erOppfyltPå(skadedato)) return diagnoseTidslinje
    }

    return diagnoseTidslinje.kombinerMed(medlemskapTidslinje) { diagnose, medlemskap ->
        if (diagnose == true && medlemskap == true) true else null
    }
}

private fun Tidslinje<Boolean>.eller(annenTidslinje: Tidslinje<Boolean>): Tidslinje<Boolean> = kombinerMed(annenTidslinje) { venstre, høyre -> if (venstre == true || høyre == true) true else null }

private fun Tidslinje<Boolean>.erOppfyltPå(tidspunkt: LocalDate): Boolean = tilPerioder().verdiPåTidspunkt(tidspunkt) == true
