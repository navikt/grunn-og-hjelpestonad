package no.nav.grunn.og.hjelpestonad.infotrygd.evaluering

import java.time.YearMonth

/** Siste evaluering (EV05/EV10) for saken et vedtak hører til. Inneholder helseopplysninger (GDPR art. 9). */
data class InfotrygdEvaluering(
    /** Måneden evalueringen ble notert i Infotrygd (999999 − ev05_aar_mnd_seq). */
    val månedNotertIInfotrygd: YearMonth,
    /** Posisjon 055, de fire første tegnene i ev10_text. */
    val diagnose1: String?,
    /** Posisjon 100, de fire første tegnene i ev10_text. */
    val diagnose2: String?,
    /** Posisjon 150, hele ev10_text. */
    val sats: String?,
    /** Posisjon 160, hele ev10_text. */
    val hjemmelbokstav: String?,
    /** Posisjon 165, MMÅÅ i ev10_text. ÅÅ 51–99 er 1951–1999, 00–50 er 2000–2050. */
    val virkningstidspunkt: YearMonth?,
    /**
     * Posisjoner vi ikke kan vise én sikker verdi for: ulike verdier i måneden, ugyldig virkningstidspunkt, eller færre
     * rader enn GH-saker. Feltene for disse posisjonene er null, og saksbehandler må lese dem i Infotrygd.
     */
    val motstridendeFelt: Set<Ev10Posisjon>,
)

/** Én rad fra ev_data_10 i siste evalueringsmåned for saken vedtaket hører til. */
data class Ev10Rad(
    /** Bare til team logs. */
    val personident: String,
    val månedNotertIInfotrygd: YearMonth,
    val vedtakId: Long,
    val ev10Id: Long,
    /** Antall ulike ev05_sak med GH/EVGH for personen i samme måned. */
    val antallSakerIMåned: Int,
    val posisjon: String,
    val tekst: String,
)

/**
 * Posisjonene i ev_data_10 vi viser. Repositoryet henter bare disse.
 *
 * Målt i dev 2026-10-20: EVGH skriver alle fem i hver evalueringsmåned, og hver GH-sak gir én rad per posisjon.
 * Andre evalueringer (EVBE, EVZ1 osv.) kan bruke de samme posisjonene, men i måneder med EVGH gir de ingen ekstra rader.
 * Se ADR-0011 før du legger til en posisjon.
 */
enum class Ev10Posisjon(
    val kode: String,
) {
    DIAGNOSE_1("055"),
    DIAGNOSE_2("100"),
    SATS("150"),
    HJEMMELBOKSTAV("160"),
    VIRKNINGSTIDSPUNKT("165"),
    ;

    companion object {
        fun fraKode(kode: String): Ev10Posisjon? = entries.find { it.kode == kode }
    }
}

/** Resultatet av å lese virkningstidspunkt fra posisjon 165. */
sealed interface LestVirkningstidspunkt {
    /** [verdi] er null når teksten er tom eller "0000". */
    data class Gyldig(
        val verdi: YearMonth?,
    ) : LestVirkningstidspunkt

    data object Ugyldig : LestVirkningstidspunkt
}

private val VIRKNINGSTIDSPUNKT_FORMAT = Regex("""\d{4}""")

/** Leser virkningstidspunkt fra posisjon 165 i ev_data_10 (MMÅÅ). Tom tekst og "0000" gir en gyldig, tom verdi. */
fun lesVirkningstidspunkt(tekst: String): LestVirkningstidspunkt {
    val verdi = tekst.trim()
    if (verdi.isEmpty() || verdi == "0000") return LestVirkningstidspunkt.Gyldig(null)
    if (!VIRKNINGSTIDSPUNKT_FORMAT.matches(verdi)) return LestVirkningstidspunkt.Ugyldig

    val måned = verdi.substring(0, 2).toInt()
    val år = verdi.substring(2, 4).toInt()
    if (måned !in 1..12) return LestVirkningstidspunkt.Ugyldig

    return LestVirkningstidspunkt.Gyldig(YearMonth.of(if (år > 50) 1900 + år else 2000 + år, måned))
}

/**
 * Setter radene fra ev_data_10 sammen til én evaluering per vedtak.
 *
 * ev_data_10 har ingen kolonne for sak, så radene gjelder personen og måneden. For hver posisjon samler vi alle radene
 * i måneden, uansett vedtaksdag og sak, og sammenligner verdien vi viser. Feltet fylles bare når alle radene har samme
 * verdi og posisjonen har minst én rad per GH-sak. Ellers havner posisjonen i [InfotrygdEvaluering.motstridendeFelt].
 * Rader med samme ev10Id telles én gang. Rader med posisjoner utenfor [Ev10Posisjon] ignoreres.
 */
fun tilEvalueringer(rader: List<Ev10Rad>): Map<Long, InfotrygdEvaluering> =
    rader
        .groupBy { it.vedtakId }
        .mapValues { (_, gruppe) -> tilEvaluering(gruppe.distinctBy { it.ev10Id }) }

private fun tilEvaluering(gruppe: List<Ev10Rad>): InfotrygdEvaluering {
    val månedNotertIInfotrygd = gruppe.map { it.månedNotertIInfotrygd }.distinct().single()
    val antallSaker = gruppe.maxOf { it.antallSakerIMåned }
    val raderPerPosisjon =
        gruppe
            .mapNotNull { rad -> Ev10Posisjon.fraKode(rad.posisjon)?.let { posisjon -> posisjon to rad } }
            .groupBy({ it.first }, { it.second })

    val diagnose1 = posisjonsverdi(raderPerPosisjon[Ev10Posisjon.DIAGNOSE_1].orEmpty(), antallSaker) { Posisjonsverdi.Sikker(it.take(4)) }
    val diagnose2 = posisjonsverdi(raderPerPosisjon[Ev10Posisjon.DIAGNOSE_2].orEmpty(), antallSaker) { Posisjonsverdi.Sikker(it.take(4)) }
    val sats = posisjonsverdi(raderPerPosisjon[Ev10Posisjon.SATS].orEmpty(), antallSaker) { Posisjonsverdi.Sikker(it) }
    val hjemmelbokstav = posisjonsverdi(raderPerPosisjon[Ev10Posisjon.HJEMMELBOKSTAV].orEmpty(), antallSaker) { Posisjonsverdi.Sikker(it) }
    val virkningstidspunkt = posisjonsverdi(raderPerPosisjon[Ev10Posisjon.VIRKNINGSTIDSPUNKT].orEmpty(), antallSaker) { tolkVirkningstidspunkt(it) }

    val motstridendeFelt =
        Ev10Posisjon.entries
            .filter {
                when (it) {
                    Ev10Posisjon.DIAGNOSE_1 -> diagnose1
                    Ev10Posisjon.DIAGNOSE_2 -> diagnose2
                    Ev10Posisjon.SATS -> sats
                    Ev10Posisjon.HJEMMELBOKSTAV -> hjemmelbokstav
                    Ev10Posisjon.VIRKNINGSTIDSPUNKT -> virkningstidspunkt
                } is Posisjonsverdi.Usikker
            }.toSet()

    return InfotrygdEvaluering(
        månedNotertIInfotrygd = månedNotertIInfotrygd,
        diagnose1 = diagnose1.verdiEllerNull(),
        diagnose2 = diagnose2.verdiEllerNull(),
        sats = sats.verdiEllerNull(),
        hjemmelbokstav = hjemmelbokstav.verdiEllerNull(),
        virkningstidspunkt = virkningstidspunkt.verdiEllerNull(),
        motstridendeFelt = motstridendeFelt,
    )
}

private fun <T> Posisjonsverdi<T>.verdiEllerNull(): T? = (this as? Posisjonsverdi.Sikker)?.verdi

/** Verdien for én posisjon i måneden, eller at vi ikke kan vise en sikker verdi. */
private sealed interface Posisjonsverdi<out T> {
    data class Sikker<T>(
        val verdi: T,
    ) : Posisjonsverdi<T>

    data object Usikker : Posisjonsverdi<Nothing>
}

/**
 * Sikker bare når posisjonen har minst én rad per GH-sak og alle radene tolkes til samme verdi. Har en sak ingen rad,
 * kan raden vi ser høre til en annen sak. Tom tekst er en verdi, så tom tekst mot innhold er usikkert.
 */
private fun <T> posisjonsverdi(
    rader: List<Ev10Rad>,
    antallSaker: Int,
    hentPosisjonsverdiFraTekstfelt: (String) -> Posisjonsverdi<T>,
): Posisjonsverdi<T> {
    if (rader.isEmpty() || rader.size < antallSaker) return Posisjonsverdi.Usikker
    return rader.map { hentPosisjonsverdiFraTekstfelt(it.tekst) }.distinct().singleOrNull() ?: Posisjonsverdi.Usikker
}

private fun tolkVirkningstidspunkt(tekst: String): Posisjonsverdi<YearMonth?> =
    when (val lest = lesVirkningstidspunkt(tekst)) {
        is LestVirkningstidspunkt.Gyldig -> Posisjonsverdi.Sikker(lest.verdi)
        LestVirkningstidspunkt.Ugyldig -> Posisjonsverdi.Usikker
    }
