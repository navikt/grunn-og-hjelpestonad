package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.felles.periode.tilTidslinje
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PerioderMedOppfylteInngangsvilkårUtleder
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingValidering
import no.nav.grunn.og.hjelpestonad.trygdetid.Trygdetid
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidRegler
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.EkstrautgifterRegler
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import java.math.BigDecimal
import java.time.YearMonth

/** Tilkjent ytelse for et sammenhengende tidsrom med samme beløp per måned. [tilOgMedMåned] er `null` når retten ikke har sluttdato. */
data class UtledetAndel(
    val fraOgMedMåned: YearMonth,
    val tilOgMedMåned: YearMonth?,
    val beløp: BigDecimal,
)

/** Hva en valideringsfeil gjelder. Trygdetid er ikke et vilkår og har derfor en egen nøkkel. */
sealed interface Valideringsnøkkel {
    data class Vilkår(
        val vilkårType: VilkårType,
    ) : Valideringsnøkkel

    data object Trygdetid : Valideringsnøkkel
}

data class Utledningsfeil(
    val nøkkel: Valideringsnøkkel,
    val melding: String,
)

/** Grunnlaget for tilkjent ytelse når den ikke er komplett. Har høyst én feil per [Valideringsnøkkel]. */
class UtledningAvTilkjentYtelseFeil(
    val feil: List<Utledningsfeil>,
) : RuntimeException("Grunnlaget for tilkjent ytelse er ikke komplett")

object TilkjentYtelseUtleder {
    const val EKSTRAUTGIFTER_IKKE_VURDERT = "Du må vurdere nødvendige ekstrautgifter."
    const val TRYGDETID_MANGLER = "Du må registrere trygdetid for hele perioden søker har rett til grunnstønad."

    fun utled(
        medlemskap: List<VilkårMedlemskap>,
        diagnoser: List<VilkårDiagnose>,
        ekstrautgifter: List<VilkårEkstrautgifter>,
        trygdetid: List<Trygdetid>,
    ): List<UtledetAndel> {
        val inngangsvilkårsfeil = VilkårVurderingValidering.valider(medlemskap, diagnoser)
        if (inngangsvilkårsfeil.isNotEmpty()) throw VilkårValideringFeil(inngangsvilkårsfeil)

        ekstrautgifter.forEach { vilkårEkstrautgifter ->
            if (!EkstrautgifterRegler.erDiagnoseneOppfyltIHelePerioden(vilkårEkstrautgifter, diagnoser)) {
                throw UgyldigInput(EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN)
            }
        }

        val perioderMedOppfylteInngangsvilkår = PerioderMedOppfylteInngangsvilkårUtleder.utled(medlemskap, diagnoser)
        if (perioderMedOppfylteInngangsvilkår.isEmpty()) return emptyList()

        val erInngangsvilkårOppfyltTidslinje = perioderMedOppfylteInngangsvilkår.map { Periode(true, it.fraOgMedDato, it.tilOgMedDato) }.tilTidslinje()
        val ekstrautgifterTidslinje = ekstrautgifter.tilTidslinje()
        val trygdetidTidslinje = trygdetid.tilTidslinje()

        val satsTidslinje = satsNårVilkåreneErOppfylt(erInngangsvilkårOppfyltTidslinje, ekstrautgifterTidslinje)

        valider(erInngangsvilkårOppfyltTidslinje, ekstrautgifterTidslinje, satsTidslinje, trygdetidTidslinje)

        val erVilkåreneOppfyltTidslinje = satsTidslinje.mapVerdi<Int, Boolean> { sats -> sats?.let { true } }

        val satsOgTrygdetidPerMåned =
            erVilkåreneOppfyltTidslinje
                .forskyvTilHeleMåneder()
                .kombinerMed(
                    satsTidslinje.forskyvTilHeleMåneder(),
                    trygdetidTidslinje.mapVerdi { it?.antallÅr }.forskyvTilHeleMåneder(),
                ) { oppfylt, sats, antallÅr ->
                    if (oppfylt == true) {
                        SatsOgTrygdetid(
                            sats = checkNotNull(sats) { "Mangler sats i en måned der vilkårene er oppfylt." },
                            trygdetid = checkNotNull(antallÅr) { "Mangler trygdetid i en måned der vilkårene er oppfylt." },
                        )
                    } else {
                        null
                    }
                }

        return satsOgTrygdetidPerMåned
            .kombinerMed(SATSBELØP_PER_MÅNED) { satsOgTrygdetid, satsbeløpeneIPerioden ->
                satsOgTrygdetid?.let {
                    utledBeløp(it, satsbeløpeneIPerioden)
                }
            }.slåSammenLikePerioder()
            .tilPerioderIkkeNull()
            .map {
                UtledetAndel(
                    fraOgMedMåned = YearMonth.from(checkNotNull(it.fom) { "En andel mangler fra og med-dato." }),
                    tilOgMedMåned = it.tom?.let(YearMonth::from),
                    beløp = it.verdi,
                )
            }
    }

    private fun satsNårVilkåreneErOppfylt(
        erInngangsvilkårOppfyltTidslinje: Tidslinje<Boolean>,
        ekstrautgifter: Tidslinje<VilkårEkstrautgifter>,
    ): Tidslinje<Int> =
        erInngangsvilkårOppfyltTidslinje.kombinerMed(ekstrautgifter) { erOppfylt, ekstrautgift ->
            if (erOppfylt == true && ekstrautgift?.erVilkårOppfylt() == true) {
                checkNotNull(ekstrautgift.sats) { "Ekstrautgifter vurdert til JA mangler sats." }.nummer
            } else {
                null
            }
        }

    private fun valider(
        erInngangsvilkårOppfyltTidslinje: Tidslinje<Boolean>,
        ekstrautgifter: Tidslinje<VilkårEkstrautgifter>,
        satsTidslinje: Tidslinje<Int>,
        trygdetid: Tidslinje<Trygdetid>,
    ) {
        val ekstrautgifterVurdert =
            erInngangsvilkårOppfyltTidslinje
                .kombinerMed(ekstrautgifter) { erInngangsvilkårOppfylt, ekstrautgift -> if (erInngangsvilkårOppfylt == true && ekstrautgift != null) true else null }
                .harVerdi()
        val hullITrygdetid =
            satsTidslinje
                .kombinerMed(trygdetid) { sats, trygdetid -> if (sats != null && trygdetid == null) true else null }
                .harVerdi()

        val feil =
            listOfNotNull(
                Utledningsfeil(Valideringsnøkkel.Vilkår(VilkårType.NØDVENDIGE_EKSTRAUTGIFTER), EKSTRAUTGIFTER_IKKE_VURDERT)
                    .takeUnless { ekstrautgifterVurdert },
                Utledningsfeil(Valideringsnøkkel.Trygdetid, TRYGDETID_MANGLER).takeIf { hullITrygdetid },
            )
        if (feil.isNotEmpty()) throw UtledningAvTilkjentYtelseFeil(feil)
    }

    /**
     * Satsbeløp × trygdetid / full trygdetid, eksakt. Bruker `divide` uten avrunding, fordi `/` i Kotlin beholder skalaen til
     * satsbeløpet og runder.
     */
    private fun utledBeløp(
        satsOgTrygdetid: SatsOgTrygdetid,
        satsbeløpeneIPerioden: List<Satsbeløp>?,
    ): BigDecimal? {
        val satsbeløp =
            satsbeløpeneIPerioden?.singleOrNull { it.sats == satsOgTrygdetid.sats }
                ?: throw UgyldigInput("Finner ikke satsbeløp for sats ${satsOgTrygdetid.sats}.")
        return (satsbeløp.månedsbeløp.toBigDecimal() * satsOgTrygdetid.trygdetid.toBigDecimal()).divide(TrygdetidRegler.FULL_TRYGDETID.toBigDecimal()).takeIf { it.signum() != 0 }
    }

    private val SATSBELØP_PER_MÅNED: Tidslinje<List<Satsbeløp>> =
        Satsbeløpene.alle
            .groupBy { it.sats }
            .values
            .map { beløpForSatsen ->
                beløpForSatsen
                    .map {
                        Periode(
                            verdi = it,
                            fom = YearMonth.from(it.gyldigFraOgMed).atDay(1),
                            tom = it.gyldigTilOgMed?.let { tom -> YearMonth.from(tom.plusDays(1)).minusMonths(1).atEndOfMonth() },
                        )
                    }.tilTidslinje()
            }.kombiner { it.toList() }
}

private data class SatsOgTrygdetid(
    val sats: Int,
    val trygdetid: Int,
)

private fun <T> Tidslinje<T>.harVerdi(): Boolean = tilPerioderIkkeNull().isNotEmpty()
