package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.oppdrag.DetaljertSimuleringResultat
import no.nav.grunn.og.hjelpestonad.oppdrag.FagOmrådeKode
import no.nav.grunn.og.hjelpestonad.oppdrag.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Regner ut nytt beløp, tidligere utbetalt, resultat, feilutbetaling og etterbetaling fra posteringene Oppdrag
 * simulerer. Basert på `SimuleringUtil.kt` i familie-ba-sak, se ADR-0018 og
 * https://confluence.adeo.no/display/TFA/Tolkning+av+simulerte+posteringer+fra+oppdragsystemet.
 *
 * Bare posteringer av typen `YTELSE` og `FEILUTBETALING` gir beløp. `MOTP`, `JUSTERING`, `FORSKUDSSKATT` og `TREKK`
 * gir ikke beløp, og frontend viser dem ikke. Ukjente navn på `mottakerType`, `posteringType` og `betalingType` gir
 * `UgyldigInput`. Ukjente navn på `fagOmrådeKode` havner i `ukjenteFagområdekoder`. Manglende eller tom `fagOmrådeKode`
 * gir `UgyldigInput`. Råsvaret blir stående som `SIMULERT`, og API svarer `FEILET`.
 */
object SimuleringOppsummerer {
    /**
     * Fagområdekoder for manuelle posteringer på grunn- og hjelpestønad.
     */
    val MANUELLE_FAGOMRÅDEKODER: Set<FagOmrådeKode> =
        setOf(
            FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD_MANUELT,
            FagOmrådeKode.HJELPESTØNAD_INFOTRYGD_MANUELT,
            FagOmrådeKode.GRUNN_OG_HJELPESTØNAD_TILBAKEKREVING_MANUELT,
        )

    private val FAGOMRÅDEKODER_FOR_YTELSEN = setOf(FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD, FagOmrådeKode.HJELPESTØNAD_INFOTRYGD)

    private val POSTERINGSTYPER_SOM_GIR_BELØP = setOf(Posteringstype.YTELSE, Posteringstype.FEILUTBETALING)

    /**
     * @param simuleringHentetDato dagen simuleringen ble hentet fra Oppdrag. Bare posteringer som forfaller denne
     * dagen eller tidligere, gir etterbetaling.
     */
    fun oppsummer(
        detaljertSimuleringResultat: DetaljertSimuleringResultat,
        simuleringHentetDato: LocalDate,
        manuelleFagområdekoder: Set<FagOmrådeKode> = MANUELLE_FAGOMRÅDEKODER,
    ): Simuleringsoppsummering {
        val simuleringsposteringer =
            detaljertSimuleringResultat.simuleringMottaker.flatMap { simuleringMottaker ->
                simuleringMottaker.simulertPostering.map { simulertPostering ->
                    tilSimuleringspostering(simulertPostering, simuleringMottaker.mottakerType)
                }
            }
        val simuleringsmåneder = lagMåneder(simuleringsposteringer, simuleringHentetDato, manuelleFagområdekoder)
        val nesteUtbetalingsmåned = finnNesteUtbetalingsmåned(simuleringsmåneder, simuleringHentetDato)
        val månederFørNesteUtbetaling =
            simuleringsmåneder.filter { nesteUtbetalingsmåned == null || it.fom < nesteUtbetalingsmåned.fom }
        val kjenteFagområdekodenavn = (manuelleFagområdekoder + FAGOMRÅDEKODER_FOR_YTELSEN).map { it.name }.toSet()

        return Simuleringsoppsummering(
            fom = simuleringsmåneder.minOfOrNull { it.fom },
            tomSisteUtbetaling = månederFørNesteUtbetaling.maxOfOrNull { it.tom },
            feilutbetaling = månederFørNesteUtbetaling.sumOf { it.beløp.feilutbetaling }.ikkeNegativ(),
            etterbetaling = månederFørNesteUtbetaling.sumOf { it.beløp.etterbetaling }.ikkeNegativ(),
            nesteUtbetaling =
                nesteUtbetalingsmåned?.let {
                    NesteUtbetaling(fom = it.fom, tom = it.tom, forfallsdato = it.forfallsdato, beløp = it.beløp.resultat)
                },
            ukjenteFagområdekoder =
                simuleringsposteringer
                    .map { it.fagområdekode }
                    .filterNot { it in kjenteFagområdekodenavn }
                    .distinct()
                    .sorted(),
            perioder = SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(simuleringsmåneder, nesteUtbetalingsmåned?.fom),
        )
    }

    /**
     * Én måned per `fom`, som `vedtakSimuleringMottakereTilSimuleringPerioder` i ba-sak. Bare `YTELSE` og `FEILUTBETALING`
     * er med i måneden, og `posteringer` har bare disse. Måneder uten dem tas ikke med. Andre posteringstyper, som `MOTP`,
     * kan ha andre datoer uten at det gir feil.
     *
     * `tom` og `forfallsdato` skal være like for alle posteringene i måneden. ba-sak antar det og tar verdiene fra
     * `posteringListe[0]`. Vi sjekker det: er de ulike, er svaret ugyldig. Råsvaret fra Oppdrag blir stående som
     * `SIMULERT`, så vi kan se hva som kom. API svarer `FEILET`.
     */
    internal fun lagMåneder(
        simuleringsposteringer: List<Simuleringspostering>,
        simuleringHentetDato: LocalDate,
        manuelleFagområdekoder: Set<FagOmrådeKode>,
    ): List<Simuleringsmåned> {
        val manuelleFagområdekodenavn = manuelleFagområdekoder.map { it.name }.toSet()

        return simuleringsposteringer
            .filter { it.posteringstype in POSTERINGSTYPER_SOM_GIR_BELØP }
            .groupBy { it.fom }
            .toSortedMap()
            .map { posteringerPerFom ->
                val posteringerIMåneden = posteringerPerFom.value
                val posteringerSomGirBeløp =
                    posteringerIMåneden.map { MånedsPostering(it, erManuell = it.fagområdekode in manuelleFagområdekodenavn) }

                Simuleringsmåned(
                    fom = posteringerPerFom.key,
                    tom =
                        posteringerSomGirBeløp.map { it.postering.tom }.distinct().singleOrNull()
                            ?: throw UgyldigInput("Posteringene i en måned i simuleringen fra Oppdrag har ulik til og med-dato."),
                    forfallsdato =
                        posteringerSomGirBeløp.map { it.postering.forfallsdato }.distinct().singleOrNull()
                            ?: throw UgyldigInput("Posteringene i en måned i simuleringen fra Oppdrag har ulik forfallsdato."),
                    beløp =
                        Simuleringsbeløp(
                            nyttBeløp = nyttBeløp(posteringerSomGirBeløp),
                            tidligereUtbetalt = tidligereUtbetalt(posteringerSomGirBeløp),
                            resultat = resultat(posteringerSomGirBeløp),
                            feilutbetaling = positivFeilutbetaling(posteringerSomGirBeløp),
                            etterbetaling = etterbetaling(posteringerSomGirBeløp, simuleringHentetDato),
                            manuellPostering = manuellPostering(posteringerSomGirBeløp),
                        ),
                    posteringer = posteringerIMåneden,
                )
            }
    }

    /** Første måned som ikke er utbetalt ennå og ikke har feilutbetaling. */
    private fun finnNesteUtbetalingsmåned(
        simuleringsmåneder: List<Simuleringsmåned>,
        simuleringHentetDato: LocalDate,
    ): Simuleringsmåned? =
        simuleringsmåneder
            .filter { it.fom > simuleringHentetDato || (it.tom > simuleringHentetDato && it.forfallsdato > simuleringHentetDato) }
            .filter { it.beløp.feilutbetaling.signum() == 0 }
            .minByOrNull { it.fom }

    private data class MånedsPostering(
        val postering: Simuleringspostering,
        val erManuell: Boolean,
    ) {
        val beløp: BigDecimal get() = postering.beløp
        val erYtelse: Boolean get() = postering.posteringstype == Posteringstype.YTELSE
        val erFeilutbetaling: Boolean get() = postering.posteringstype == Posteringstype.FEILUTBETALING
    }

    private fun nyttBeløp(posteringer: List<MånedsPostering>): BigDecimal {
        val sumPositiveYtelser = posteringer.filter { it.erYtelse && !it.erManuell && it.beløp > BigDecimal.ZERO }.sumOf { it.beløp }
        val feilutbetaling = feilutbetaling(posteringer, inkluderManuellePosteringer = false)
        return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
    }

    private fun tidligereUtbetalt(posteringer: List<MånedsPostering>): BigDecimal {
        val sumNegativeYtelser = posteringer.filter { it.erYtelse && !it.erManuell && it.beløp < BigDecimal.ZERO }.sumOf { it.beløp }
        val feilutbetaling = feilutbetaling(posteringer, inkluderManuellePosteringer = false)
        // Manuelle posteringer justerer det som faktisk ble utbetalt. En negativ sum øker tidligere utbetalt.
        val sumManuellePosteringer = -manuellPostering(posteringer)
        return if (feilutbetaling < BigDecimal.ZERO) {
            -(sumNegativeYtelser - feilutbetaling)
        } else {
            -sumNegativeYtelser + sumManuellePosteringer
        }
    }

    private fun manuellPostering(posteringer: List<MånedsPostering>): BigDecimal {
        val sumManuelleYtelser = posteringer.filter { it.erYtelse && it.erManuell }.sumOf { it.beløp }
        val sumManuelleFeilutbetalinger = posteringer.filter { it.erFeilutbetaling && it.erManuell }.sumOf { it.beløp }
        return sumManuelleYtelser - sumManuelleFeilutbetalinger
    }

    private fun feilutbetaling(
        posteringer: List<MånedsPostering>,
        inkluderManuellePosteringer: Boolean,
    ): BigDecimal = posteringer.filter { it.erFeilutbetaling && (inkluderManuellePosteringer || !it.erManuell) }.sumOf { it.beløp }

    private fun positivFeilutbetaling(posteringer: List<MånedsPostering>): BigDecimal = posteringer.filter { it.erFeilutbetaling && it.beløp > BigDecimal.ZERO }.sumOf { it.beløp }

    private fun negativFeilutbetaling(posteringer: List<MånedsPostering>): BigDecimal = posteringer.filter { it.erFeilutbetaling && it.beløp < BigDecimal.ZERO }.sumOf { it.beløp }

    private fun resultat(posteringer: List<MånedsPostering>): BigDecimal {
        val feilutbetaling = feilutbetaling(posteringer, inkluderManuellePosteringer = true)
        return if (feilutbetaling > BigDecimal.ZERO) -feilutbetaling else nyttBeløp(posteringer) - tidligereUtbetalt(posteringer)
    }

    /**
     * Bare posteringer som har forfalt, gir etterbetaling. En negativ feilutbetaling betyr at en tidligere registrert
     * feilutbetaling blir mindre. Den trekkes fra, fordi Oppdrag bruker beløpet til å redusere feilutbetalingen.
     */
    private fun etterbetaling(
        posteringer: List<MånedsPostering>,
        simuleringHentetDato: LocalDate,
    ): BigDecimal {
        val forfaltePosteringer = posteringer.filter { it.postering.forfallsdato <= simuleringHentetDato }
        if (feilutbetaling(forfaltePosteringer, inkluderManuellePosteringer = true) > BigDecimal.ZERO) return BigDecimal.ZERO
        return (resultat(forfaltePosteringer) + negativFeilutbetaling(forfaltePosteringer)).ikkeNegativ()
    }

    private fun BigDecimal.ikkeNegativ(): BigDecimal = if (this < BigDecimal.ZERO) BigDecimal.ZERO else this

    /** Verdien alle posteringene i måneden har felles. Kaster når de er ulike. */
    private fun <T> List<MånedsPostering>.likVerdi(
        feltnavn: String,
        hentVerdi: (Simuleringspostering) -> T,
    ): T =
        map { hentVerdi(it.postering) }.distinct().singleOrNull()
            ?: throw UgyldigInput("Posteringene i en måned i simuleringen fra Oppdrag har ulik $feltnavn.")

    private fun tilSimuleringspostering(
        simulertPostering: SimulertPostering,
        mottakertype: String?,
    ) = Simuleringspostering(
        fagområdekode = simulertPostering.fagOmrådeKode?.takeIf { it.isNotBlank() } ?: manglerFelt("fagOmrådeKode"),
        mottakertype = mottakertype?.let { tilEnum<Mottakertype>("mottakerType", it) },
        fom = simulertPostering.fom ?: manglerFelt("fom"),
        tom = simulertPostering.tom ?: manglerFelt("tom"),
        forfallsdato = simulertPostering.forfallsdato ?: manglerFelt("forfallsdato"),
        posteringstype = tilEnum<Posteringstype>("posteringType", simulertPostering.posteringType ?: manglerFelt("posteringType")),
        betalingstype = simulertPostering.betalingType?.let { tilEnum<Betalingstype>("betalingType", it) },
        klassekode = simulertPostering.klassekode,
        beløp = simulertPostering.beløp ?: manglerFelt("beløp"),
        erFeilkonto = simulertPostering.erFeilkonto,
        utenInntrekk = simulertPostering.utenInntrekk,
    )

    private fun manglerFelt(feltnavn: String): Nothing = throw UgyldigInput("En postering i simuleringen fra Oppdrag mangler $feltnavn.")

    /** Navnet fra Oppdrag gjøres om til enum-verdien. Et navn vi ikke kjenner, kaster [UgyldigInput]. */
    private inline fun <reified T : Enum<T>> tilEnum(
        feltnavn: String,
        verdiFraOppdrag: String,
    ): T =
        enumValues<T>().firstOrNull { it.name == verdiFraOppdrag }
            ?: throw UgyldigInput("En postering i simuleringen fra Oppdrag har ukjent $feltnavn: $verdiFraOppdrag.")
}
