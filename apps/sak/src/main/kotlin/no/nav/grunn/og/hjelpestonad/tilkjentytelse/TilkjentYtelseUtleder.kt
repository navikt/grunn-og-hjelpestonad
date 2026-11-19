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
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsfeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsnøkkel
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingValidering
import no.nav.grunn.og.hjelpestonad.trygdetid.Trygdetid
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidRegler
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

object TilkjentYtelseUtleder {
    const val EKSTRAUTGIFTER_IKKE_VURDERT = "Du må vurdere nødvendige ekstrautgifter."
    const val TRYGDETID_MANGLER = "Du må registrere trygdetid for hele perioden søker har rett til grunnstønad."

    private val NEVNER = BigDecimal(TrygdetidRegler.MAKS_TELLER)

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
                throw VilkårValideringFeil(
                    listOf(
                        Valideringsfeil(
                            nøkkel = Valideringsnøkkel.NØDVENDIGE_EKSTRAUTGIFTER,
                            melding = EkstrautgifterRegler.DIAGNOSE_IKKE_OPPFYLT_I_HELE_PERIODEN,
                        ),
                    ),
                )
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

        val satsOgTellerPerMåned =
            erVilkåreneOppfyltTidslinje
                .forskyvTilHeleMåneder()
                .kombinerMed(
                    satsTidslinje.forskyvTilHeleMåneder(),
                    trygdetidTidslinje.mapVerdi { it?.teller }.forskyvTilHeleMåneder(),
                ) { oppfylt, sats, teller ->
                    if (oppfylt == true) {
                        SatsOgTeller(
                            sats = checkNotNull(sats) { "Mangler sats i en måned der vilkårene er oppfylt." },
                            teller = checkNotNull(teller) { "Mangler trygdetid i en måned der vilkårene er oppfylt." },
                        )
                    } else {
                        null
                    }
                }

        return satsOgTellerPerMåned
            .kombinerMed(SATSBELØP_PER_MÅNED) { satsOgTeller, satsbeløpeneIPerioden ->
                satsOgTeller?.let {
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
                Valideringsfeil(Valideringsnøkkel.NØDVENDIGE_EKSTRAUTGIFTER, EKSTRAUTGIFTER_IKKE_VURDERT)
                    .takeUnless { ekstrautgifterVurdert },
                Valideringsfeil(Valideringsnøkkel.TRYGDETID, TRYGDETID_MANGLER).takeIf { hullITrygdetid },
            )
        if (feil.isNotEmpty()) throw VilkårValideringFeil(feil)
    }

    /**
     * Satsbeløp × teller / 40, eksakt. Bruker `divide` uten avrunding, fordi `/` i Kotlin beholder skalaen til
     * satsbeløpet og runder.
     */
    private fun utledBeløp(
        satsOgTeller: SatsOgTeller,
        satsbeløpeneIPerioden: List<Satsbeløp>?,
    ): BigDecimal? {
        val satsbeløp =
            satsbeløpeneIPerioden?.singleOrNull { it.sats == satsOgTeller.sats }
                ?: throw UgyldigInput("Finner ikke satsbeløp for sats ${satsOgTeller.sats}.")
        return (satsbeløp.månedsbeløp.toBigDecimal() * satsOgTeller.teller.toBigDecimal()).divide(NEVNER).takeIf { it.signum() != 0 }
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

private data class SatsOgTeller(
    val sats: Int,
    val teller: Int,
)

private fun <T> Tidslinje<T>.harVerdi(): Boolean = tilPerioderIkkeNull().isNotEmpty()
