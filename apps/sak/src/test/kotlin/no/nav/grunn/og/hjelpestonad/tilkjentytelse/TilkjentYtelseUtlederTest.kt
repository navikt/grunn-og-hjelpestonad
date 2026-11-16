package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.trygdetid.Trygdetid
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidKilde
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Sats
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val DIABETES_TYPE_1 = "E109"

/**
 * Reglene i docs/tilkjent-ytelse/utledning-av-tilkjent-ytelse.md, med én test per regel. Ren test uten Spring og uten
 * mockk, jf. ADR-0008. Satsbeløpene fra 2024: sats 1 er 752, sats 2 er 1 146, sats 3 er 1 502 og sats 4 er 2 212 kr.
 */
class TilkjentYtelseUtlederTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `eksempelet i dokumentet om utledning av tilkjent ytelse`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-14", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-15", tilOgMedDato = null, sats = Sats.SATS_4),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-09-09", teller = 40),
                        trygdetid(fraOgMedDato = "2026-09-10", tilOgMedDato = null, teller = 30, kilde = TrygdetidKilde.ALDERSPENSJON),
                    ),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-05", beløp = "1146"),
            andel(fom = "2026-06", tom = "2026-09", beløp = "2212"),
            andel(fom = "2026-10", tom = null, beløp = "1659"),
        )
    }

    @Test
    fun `uten perioder med oppfylte inngangsvilkår gir ingen andeler og ingen feil`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null, vurdering = Vurdering.NEI)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
            )

        assertThat(andeler).isEmpty()
    }

    @Test
    fun `perioder med oppfylte inngangsvilkår uten vurderte ekstrautgifter gir feil på ekstrautgiftene`() {
        val feil =
            assertFailsWith<UtledningAvTilkjentYtelseFeil> {
                utled(
                    medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                    diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                    trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 40)),
                )
            }.feil

        assertThat(feil).containsExactly(
            Utledningsfeil(
                nøkkel = Valideringsnøkkel.Vilkår(vilkårType = VilkårType.NØDVENDIGE_EKSTRAUTGIFTER),
                melding = TilkjentYtelseUtleder.EKSTRAUTGIFTER_IKKE_VURDERT,
            ),
        )
    }

    @Test
    fun `ekstrautgifter vurdert bare utenfor inngangsvilkårene regnes som ikke vurdert`() {
        val feil =
            assertFailsWith<UtledningAvTilkjentYtelseFeil> {
                utled(
                    medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30")),
                    diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30")),
                    ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-07-01", tilOgMedDato = null, sats = Sats.SATS_2)),
                    trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 40)),
                )
            }.feil

        assertThat(feil.map { it.nøkkel }).containsExactly(Valideringsnøkkel.Vilkår(vilkårType = VilkårType.NØDVENDIGE_EKSTRAUTGIFTER))
    }

    @Test
    fun `hull i trygdetiden der søker har rett gir én feil på trygdetid`() {
        val feil =
            assertFailsWith<UtledningAvTilkjentYtelseFeil> {
                utled(
                    medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                    diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                    ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_2)),
                    trygdetid =
                        listOf(
                            trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-05-31", teller = 40),
                            trygdetid(fraOgMedDato = "2026-07-01", tilOgMedDato = "2026-08-31", teller = 40),
                        ),
                )
            }.feil

        assertThat(feil).containsExactly(Utledningsfeil(nøkkel = Valideringsnøkkel.Trygdetid, melding = TilkjentYtelseUtleder.TRYGDETID_MANGLER))
    }

    @Test
    fun `hull i ekstrautgiftene betyr nei og krever ikke trygdetid`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-31", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-04-01", tilOgMedDato = "2026-04-30", sats = null),
                        ekstrautgifter(fraOgMedDato = "2026-07-01", tilOgMedDato = null, sats = Sats.SATS_2),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-31", teller = 40),
                        trygdetid(fraOgMedDato = "2026-07-01", tilOgMedDato = null, teller = 40),
                    ),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-02", tom = "2026-03", beløp = "1146"),
            andel(fom = "2026-08", tom = null, beløp = "1146"),
        )
    }

    @Test
    fun `ekstrautgifter og trygdetid utenfor inngangsvilkårene påvirker ikke resultatet`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30")),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30")),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2025-01-01", tilOgMedDato = "2026-06-30", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-07-01", tilOgMedDato = null, sats = Sats.SATS_6),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2025-06-01", tilOgMedDato = "2025-12-31", teller = 10),
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30", teller = 40),
                    ),
            )

        assertAndeler(faktiske = andeler, andel(fom = "2026-04", tom = "2026-06", beløp = "1146"))
    }

    @Test
    fun `første ledd - retten gjelder fra måneden etter, også når den oppstår den 1`() {
        val fraDenFørste = utledMedSats2OgFullTrygdetid(fraOgMed = "2026-03-01", tilOgMed = null)
        val fraMidtIMåneden = utledMedSats2OgFullTrygdetid(fraOgMed = "2026-03-15", tilOgMed = null)

        assertAndeler(faktiske = fraDenFørste, andel(fom = "2026-04", tom = null, beløp = "1146"))
        assertAndeler(faktiske = fraMidtIMåneden, andel(fom = "2026-04", tom = null, beløp = "1146"))
    }

    @Test
    fun `sjette ledd - utbetalingen stopper ved utgangen av måneden retten faller bort`() {
        val tilSisteDagIMåneden = utledMedSats2OgFullTrygdetid(fraOgMed = "2026-03-01", tilOgMed = "2026-09-30")
        val tilMidtIMåneden = utledMedSats2OgFullTrygdetid(fraOgMed = "2026-03-01", tilOgMed = "2026-09-15")

        assertAndeler(faktiske = tilSisteDagIMåneden, andel(fom = "2026-04", tom = "2026-09", beløp = "1146"))
        assertAndeler(faktiske = tilMidtIMåneden, andel(fom = "2026-04", tom = "2026-09", beløp = "1146"))
    }

    @Test
    fun `ny rett etter et hull gjelder fra måneden etter, også når den forrige retten ble utbetalt hele måneden`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-14", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-03-20", tilOgMedDato = "2026-05-10", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-10", tilOgMedDato = null, sats = Sats.SATS_2),
                    ),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-01-01", tilOgMedDato = null, teller = 40)),
            )

        // Mars er siste måned for den første retten, og april er første måned for den andre. Juni er måneden den
        // tredje retten oppstår, og gir ingen utbetaling.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-02", tom = "2026-05", beløp = "1146"),
            andel(fom = "2026-07", tom = null, beløp = "1146"),
        )
    }

    @Test
    fun `ny rett med høyere sats i måneden den forrige faller bort gir ikke den nye satsen før måneden etter`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-14", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-03-20", tilOgMedDato = null, sats = Sats.SATS_4),
                    ),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-01-01", tilOgMedDato = null, teller = 40)),
            )

        // Mars er siste måned for den første retten. Den nye retten oppstår i mars og gjelder fra april.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-02", tom = "2026-03", beløp = "1146"),
            andel(fom = "2026-04", tom = null, beløp = "2212"),
        )
    }

    @Test
    fun `økning av trygdetidsbrøken mellom to perioder med oppfylte vilkår gjelder fra samme måned`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-01-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-14", sats = Sats.SATS_4),
                        ekstrautgifter(fraOgMedDato = "2026-03-20", tilOgMedDato = null, sats = Sats.SATS_4),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-01-01", tilOgMedDato = "2026-03-16", teller = 30),
                        trygdetid(fraOgMedDato = "2026-03-17", tilOgMedDato = null, teller = 40),
                    ),
            )

        // Trygdetiden kuttes ikke etter vilkårene, så økningen 17. mars gjelder fra mars. Ikke bekreftet av fag, se
        // ticket 21.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-02", tom = "2026-02", beløp = "1659"),
            andel(fom = "2026-03", tom = null, beløp = "2212"),
        )
    }

    @Test
    fun `fjerde ledd - høyere sats gjelder fra samme måned`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-14", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-15", tilOgMedDato = null, sats = Sats.SATS_4),
                    ),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 40)),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-05", beløp = "1146"),
            andel(fom = "2026-06", tom = null, beløp = "2212"),
        )
    }

    @Test
    fun `femte ledd - lavere sats gjelder fra måneden etter`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-14", sats = Sats.SATS_4),
                        ekstrautgifter(fraOgMedDato = "2026-06-15", tilOgMedDato = null, sats = Sats.SATS_2),
                    ),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 40)),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-06", beløp = "2212"),
            andel(fom = "2026-07", tom = null, beløp = "1146"),
        )
    }

    @Test
    fun `fjerde ledd - høyere trygdetidsbrøk gjelder fra samme måned`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_4)),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-09-09", teller = 30),
                        trygdetid(fraOgMedDato = "2026-09-10", tilOgMedDato = null, teller = 40),
                    ),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-08", beløp = "1659"),
            andel(fom = "2026-09", tom = null, beløp = "2212"),
        )
    }

    @Test
    fun `femte ledd - lavere trygdetidsbrøk gjelder fra måneden etter, også når den gjelder fra den 1`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_4)),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-08-31", teller = 40),
                        trygdetid(fraOgMedDato = "2026-09-01", tilOgMedDato = null, teller = 30),
                    ),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-09", beløp = "2212"),
            andel(fom = "2026-10", tom = null, beløp = "1659"),
        )
    }

    @Test
    fun `økning av sats og reduksjon av trygdetidsbrøk i samme måned vurderes hver for seg`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-14", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-15", tilOgMedDato = null, sats = Sats.SATS_4),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-14", teller = 40),
                        trygdetid(fraOgMedDato = "2026-06-15", tilOgMedDato = null, teller = 30),
                    ),
            )

        // Juni: ny sats 4 med gammel brøk 40/40. Fra juli: sats 4 med 30/40.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-05", beløp = "1146"),
            andel(fom = "2026-06", tom = "2026-06", beløp = "2212"),
            andel(fom = "2026-07", tom = null, beløp = "1659"),
        )
    }

    @Test
    fun `nytt satsbeløp fra Stortinget gjelder fra måneden i listen`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2023-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2023-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2023-03-01", tilOgMedDato = null, sats = Sats.SATS_1)),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2023-03-01", tilOgMedDato = null, teller = 40)),
            )

        assertAndeler(
            faktiske = andeler,
            andel(fom = "2023-04", tom = "2023-06", beløp = "705"),
            andel(fom = "2023-07", tom = "2023-12", beløp = "724"),
            andel(fom = "2024-01", tom = null, beløp = "752"),
        )
    }

    @Test
    fun `beløpet er satsbeløp ganger teller delt på 40, eksakt uten avrunding`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_5)),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 1)),
            )

        // 2 997 × 1 / 40 = 74,925
        assertThat(andeler.single().beløp).isEqualByComparingTo("74.925")
    }

    @Test
    fun `en måned med 0 kr gir ingen andel`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_2)),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30", teller = 40),
                        trygdetid(fraOgMedDato = "2026-07-01", tilOgMedDato = "2026-09-30", teller = 0),
                        trygdetid(fraOgMedDato = "2026-10-01", tilOgMedDato = null, teller = 40),
                    ),
            )

        // Reduksjonen til 0/40 den 1. juli gjelder fra august. Økningen den 1. oktober gjelder fra oktober.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-07", beløp = "1146"),
            andel(fom = "2026-10", tom = null, beløp = "1146"),
        )
    }

    @Test
    fun `måneder med likt beløp slås sammen til én andel`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-05-31", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-01", tilOgMedDato = null, sats = Sats.SATS_2),
                    ),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-07-31", teller = 40),
                        trygdetid(fraOgMedDato = "2026-08-01", tilOgMedDato = null, teller = 40, kilde = TrygdetidKilde.ALDERSPENSJON),
                    ),
            )

        assertAndeler(faktiske = andeler, andel(fom = "2026-04", tom = null, beløp = "1146"))
    }

    @Test
    fun `flere endringer av samme sats i én måned gir høyeste sats i måneden og siste sats fra måneden etter`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter =
                    listOf(
                        ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-09", sats = Sats.SATS_4),
                        ekstrautgifter(fraOgMedDato = "2026-06-10", tilOgMedDato = "2026-06-19", sats = Sats.SATS_2),
                        ekstrautgifter(fraOgMedDato = "2026-06-20", tilOgMedDato = null, sats = Sats.SATS_3),
                    ),
                trygdetid = listOf(trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = null, teller = 40)),
            )

        // Reduksjonen til sats 2 venter til juli, og da er satsen alt økt til 3.
        assertAndeler(
            faktiske = andeler,
            andel(fom = "2026-04", tom = "2026-06", beløp = "2212"),
            andel(fom = "2026-07", tom = null, beløp = "1502"),
        )
    }

    @Test
    fun `løpende rett med trygdetid 0 av 40 gir siste andel med sluttdato`() {
        val andeler =
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "2026-03-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "2026-03-01", tilOgMedDato = null, sats = Sats.SATS_2)),
                trygdetid =
                    listOf(
                        trygdetid(fraOgMedDato = "2026-03-01", tilOgMedDato = "2026-06-30", teller = 40),
                        trygdetid(fraOgMedDato = "2026-07-01", tilOgMedDato = null, teller = 0, kilde = TrygdetidKilde.ALDERSPENSJON),
                    ),
            )

        assertAndeler(faktiske = andeler, andel(fom = "2026-04", tom = "2026-07", beløp = "1146"))
    }

    @Test
    fun `mangler satsbeløp for en måned med rett gir ugyldig input`() {
        // Satsbeløpene starter 1.1.1997. Retten oppstår i november 1996 og gjelder fra desember.
        assertFailsWith<UgyldigInput> {
            utled(
                medlemskap = listOf(medlemskap(fraOgMedDato = "1996-11-01", tilOgMedDato = null)),
                diagnoser = listOf(diagnose(fraOgMedDato = "1996-11-01", tilOgMedDato = null)),
                ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = "1996-11-01", tilOgMedDato = null, sats = Sats.SATS_2)),
                trygdetid = listOf(trygdetid(fraOgMedDato = "1996-11-01", tilOgMedDato = null, teller = 40)),
            )
        }
    }

    private fun utledMedSats2OgFullTrygdetid(
        fraOgMed: String,
        tilOgMed: String?,
    ) = utled(
        medlemskap = listOf(medlemskap(fraOgMedDato = fraOgMed, tilOgMedDato = tilOgMed)),
        diagnoser = listOf(diagnose(fraOgMedDato = fraOgMed, tilOgMedDato = tilOgMed)),
        ekstrautgifter = listOf(ekstrautgifter(fraOgMedDato = fraOgMed, tilOgMedDato = tilOgMed, sats = Sats.SATS_2)),
        trygdetid = listOf(trygdetid(fraOgMedDato = fraOgMed, tilOgMedDato = tilOgMed, teller = 40)),
    )

    private fun utled(
        medlemskap: List<VilkårMedlemskap>,
        diagnoser: List<VilkårDiagnose>,
        ekstrautgifter: List<VilkårEkstrautgifter> = emptyList(),
        trygdetid: List<Trygdetid> = emptyList(),
    ) = TilkjentYtelseUtleder.utled(medlemskap = medlemskap, diagnoser = diagnoser, ekstrautgifter = ekstrautgifter, trygdetid = trygdetid)

    private fun medlemskap(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårMedlemskap(
        behandlingId = behandlingId,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = vurdering,
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun diagnose(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = DIABETES_TYPE_1,
        tekst = "Tekst fra kodeverket",
        vurdering = Vurdering.JA,
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun ekstrautgifter(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        sats: Sats?,
        vurdering: Vurdering = if (sats == null) Vurdering.NEI else Vurdering.JA,
    ) = VilkårEkstrautgifter(
        behandlingId = behandlingId,
        vurdering = vurdering,
        sats = sats,
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun trygdetid(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        teller: Int,
        kilde: TrygdetidKilde = if (teller == 40) TrygdetidKilde.INGEN else TrygdetidKilde.UFØRETRYGD,
    ) = Trygdetid(
        behandlingId = behandlingId,
        teller = teller,
        kilde = kilde,
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun andel(
        fom: String,
        tom: String?,
        beløp: String,
    ) = UtledetAndel(fom = YearMonth.parse(fom), tom = tom?.let { YearMonth.parse(it) }, beløp = BigDecimal(beløp))

    /** Sammenligner beløp med `compareTo`, så 1146 og 1146.000 er like. */
    private fun assertAndeler(
        faktiske: List<UtledetAndel>,
        vararg forventede: UtledetAndel,
    ) {
        assertThat(faktiske)
            .usingRecursiveFieldByFieldElementComparator(
                RecursiveComparisonConfiguration
                    .builder()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal::class.java)
                    .build(),
            ).containsExactly(*forventede)
    }

    private fun dato(dato: String?) = dato?.let { LocalDate.parse(it) }
}
