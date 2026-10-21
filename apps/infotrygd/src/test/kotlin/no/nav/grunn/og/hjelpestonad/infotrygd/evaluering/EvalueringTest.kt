package no.nav.grunn.og.hjelpestonad.infotrygd.evaluering

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvalueringTest {
    private val alleFemPosisjoner =
        listOf(
            rad("055", "G400"),
            rad("100", "G401"),
            rad("150", "0123"),
            rad("160", "B"),
            rad("165", "0826"),
        )

    @Test
    fun `alle posisjonene havner i riktig felt`() {
        val evaluering = tilEvalueringer(alleFemPosisjoner).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123", evaluering.sats)
        assertEquals("B", evaluering.hjemmelbokstav)
        assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `diagnose 1 og 2 kuttes til fire tegn, sats og hjemmelbokstav beholder hele teksten`() {
        val evaluering =
            tilEvalueringer(
                listOf(
                    rad("055", "G400 epilepsi"),
                    rad("100", "G401 annen diagnose"),
                    rad("150", "0123 sats med mellomrom"),
                    rad("160", "B  hjemmel"),
                    rad("165", "0826"),
                ),
            ).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123 sats med mellomrom", evaluering.sats)
        assertEquals("B  hjemmel", evaluering.hjemmelbokstav)
    }

    @Test
    fun `posisjoner utenfor Ev10Posisjon ignoreres`() {
        val evaluering =
            tilEvalueringer(alleFemPosisjoner + listOf(rad("050", "T050"), rad("200", "K900"), rad("999", "T999"))).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `posisjon uten rad blir motstridende`() {
        val evaluering = tilEvalueringer(listOf(rad("055", "G400"))).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertNull(evaluering.diagnose2)
        assertNull(evaluering.sats)
        assertNull(evaluering.hjemmelbokstav)
        assertNull(evaluering.virkningstidspunkt)
        assertEquals(
            setOf(Ev10Posisjon.DIAGNOSE_2, Ev10Posisjon.SATS, Ev10Posisjon.HJEMMELBOKSTAV, Ev10Posisjon.VIRKNINGSTIDSPUNKT),
            evaluering.motstridendeFelt,
        )
    }

    @Test
    fun `ulik tekst i samme posisjon gir motstridende felt, uansett vedtaksdag`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "160" } +
                    listOf(rad("160", "B", ev10Id = 1), rad("160", "C", ev10Id = 2)),
            ).values.single()

        assertNull(evaluering.hjemmelbokstav)
        assertEquals(setOf(Ev10Posisjon.HJEMMELBOKSTAV), evaluering.motstridendeFelt)
    }

    @Test
    fun `bare posisjonen med ulike verdier blir null, de andre feltene fylles`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "055" } +
                    listOf(rad("055", "G400", ev10Id = 1), rad("055", "H500", ev10Id = 2)),
            ).values.single()

        assertNull(evaluering.diagnose1)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123", evaluering.sats)
        assertEquals("B", evaluering.hjemmelbokstav)
        assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
        assertEquals(setOf(Ev10Posisjon.DIAGNOSE_1), evaluering.motstridendeFelt)
    }

    @Test
    fun `tom tekst mot innhold gir motstridende felt`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "100" } +
                    listOf(rad("100", "", ev10Id = 1), rad("100", "G401", ev10Id = 2)),
            ).values.single()

        assertNull(evaluering.diagnose2)
        assertEquals(setOf(Ev10Posisjon.DIAGNOSE_2), evaluering.motstridendeFelt)
    }

    @Test
    fun `tom tekst i alle radene er en sikker, tom verdi`() {
        val evaluering =
            tilEvalueringer(alleFemPosisjoner.filterNot { it.posisjon == "100" } + rad("100", "")).values.single()

        assertEquals("", evaluering.diagnose2)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `diagnoser sammenlignes på de fire første tegnene`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "055" } +
                    listOf(rad("055", "G400 epilepsi", ev10Id = 1), rad("055", "G400 annen tekst", ev10Id = 2)),
            ).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `virkningstidspunkt sammenlignes som måned, så tom og 0000 er like`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "165" } +
                    listOf(rad("165", "", ev10Id = 1), rad("165", "0000", ev10Id = 2)),
            ).values.single()

        assertNull(evaluering.virkningstidspunkt)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `ugyldig virkningstidspunkt gir motstridende felt`() {
        val evaluering =
            tilEvalueringer(alleFemPosisjoner.filterNot { it.posisjon == "165" } + rad("165", "1399")).values.single()

        assertNull(evaluering.virkningstidspunkt)
        assertEquals("G400", evaluering.diagnose1)
        assertEquals(setOf(Ev10Posisjon.VIRKNINGSTIDSPUNKT), evaluering.motstridendeFelt)
    }

    @Test
    fun `ugyldig og gyldig virkningstidspunkt i samme måned gir motstridende felt`() {
        val evaluering =
            tilEvalueringer(
                alleFemPosisjoner.filterNot { it.posisjon == "165" } +
                    listOf(rad("165", "1399", ev10Id = 1), rad("165", "0826", ev10Id = 2)),
            ).values.single()

        assertNull(evaluering.virkningstidspunkt)
        assertEquals(setOf(Ev10Posisjon.VIRKNINGSTIDSPUNKT), evaluering.motstridendeFelt)
    }

    @Test
    fun `flere saker med samme verdi i alle posisjonene gir alle feltene`() {
        val evaluering =
            tilEvalueringer(
                listOf("055" to "G400", "100" to "G401", "150" to "0123", "160" to "B", "165" to "0826")
                    .flatMap { (posisjon, tekst) ->
                        listOf(
                            rad(posisjon, tekst, ev10Id = "1$posisjon".toLong(), antallSakerIMåned = 2),
                            rad(posisjon, tekst, ev10Id = "2$posisjon".toLong(), antallSakerIMåned = 2),
                        )
                    },
            ).values.single()

        assertEquals("G400", evaluering.diagnose1)
        assertEquals("G401", evaluering.diagnose2)
        assertEquals("0123", evaluering.sats)
        assertEquals("B", evaluering.hjemmelbokstav)
        assertEquals(YearMonth.of(2026, 8), evaluering.virkningstidspunkt)
        assertEquals(emptySet(), evaluering.motstridendeFelt)
    }

    @Test
    fun `flere saker med ulik verdi gir motstridende felt`() {
        val evaluering =
            tilEvalueringer(
                listOf(
                    rad("055", "G400", ev10Id = 1, antallSakerIMåned = 2),
                    rad("055", "H500", ev10Id = 2, antallSakerIMåned = 2),
                    rad("150", "0123", ev10Id = 3, antallSakerIMåned = 2),
                    rad("150", "0123", ev10Id = 4, antallSakerIMåned = 2),
                ),
            ).values.single()

        assertNull(evaluering.diagnose1)
        assertEquals("0123", evaluering.sats)
        assertTrue(Ev10Posisjon.DIAGNOSE_1 in evaluering.motstridendeFelt)
        assertFalse(Ev10Posisjon.SATS in evaluering.motstridendeFelt)
    }

    @Test
    fun `færre rader enn saker gir motstridende felt, selv om verdien er lik`() {
        val evaluering =
            tilEvalueringer(
                listOf(
                    rad("100", "G401", ev10Id = 1, antallSakerIMåned = 2),
                    rad("055", "G400", ev10Id = 2, antallSakerIMåned = 2),
                    rad("055", "G400", ev10Id = 3, antallSakerIMåned = 2),
                ),
            ).values.single()

        assertNull(evaluering.diagnose2)
        assertEquals("G400", evaluering.diagnose1)
        assertTrue(Ev10Posisjon.DIAGNOSE_2 in evaluering.motstridendeFelt)
        assertFalse(Ev10Posisjon.DIAGNOSE_1 in evaluering.motstridendeFelt)
    }

    @Test
    fun `rader med samme ev10Id telles én gang, også i telleregelen`() {
        val evaluering =
            tilEvalueringer(
                listOf(
                    rad("055", "G400", ev10Id = 1, antallSakerIMåned = 2),
                    rad("055", "G400", ev10Id = 1, antallSakerIMåned = 2),
                ),
            ).values.single()

        assertNull(evaluering.diagnose1)
        assertTrue(Ev10Posisjon.DIAGNOSE_1 in evaluering.motstridendeFelt)
    }

    @Test
    fun `gir én evaluering per vedtak`() {
        val evalueringer =
            tilEvalueringer(
                listOf(
                    rad("055", "G400", vedtakId = 1, ev10Id = 1),
                    rad("100", "G401", vedtakId = 1, ev10Id = 2),
                    rad("055", "H500", vedtakId = 2, ev10Id = 3),
                ),
            )

        assertEquals(setOf(1L, 2L), evalueringer.keys)
        assertEquals("G400", evalueringer.getValue(1L).diagnose1)
        assertEquals("G401", evalueringer.getValue(1L).diagnose2)
        assertEquals("H500", evalueringer.getValue(2L).diagnose1)
        assertNull(evalueringer.getValue(2L).diagnose2)
    }

    @Test
    fun `måned fylles også når felt er motstridende`() {
        val evaluering =
            tilEvalueringer(
                listOf(
                    rad("055", "G400", ev10Id = 1),
                    rad("055", "H500", ev10Id = 2),
                    rad("150", "0123", ev10Id = 3),
                ),
            ).values.single()

        assertEquals(YearMonth.of(2025, 10), evaluering.månedNotertIInfotrygd)
    }

    @Test
    fun `tom liste gir tomt kart`() {
        assertEquals(emptyMap<Long, InfotrygdEvaluering>(), tilEvalueringer(emptyList()))
    }

    @ParameterizedTest(name = "\"{0}\" gir {1}")
    @MethodSource("gyldigeVirkningstidspunkt")
    fun `leser virkningstidspunkt fra MMÅÅ`(
        tekst: String,
        forventet: YearMonth?,
    ) {
        assertEquals(LestVirkningstidspunkt.Gyldig(forventet), lesVirkningstidspunkt(tekst))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1326", "0026", "08 6", "826", "08265", "abcd"])
    fun `ugyldig virkningstidspunkt gir Ugyldig`(tekst: String) {
        assertEquals(LestVirkningstidspunkt.Ugyldig, lesVirkningstidspunkt(tekst))
    }

    private fun rad(
        posisjon: String,
        tekst: String,
        ev10Id: Long = posisjon.toLong(),
        vedtakId: Long = 1L,
        måned: YearMonth = YearMonth.of(2025, 10),
        antallSakerIMåned: Int = 1,
    ) = Ev10Rad(
        vedtakId = vedtakId,
        ev10Id = ev10Id,
        personident = "01017012345",
        månedNotertIInfotrygd = måned,
        antallSakerIMåned = antallSakerIMåned,
        posisjon = posisjon,
        tekst = tekst,
    )

    companion object {
        @JvmStatic
        fun gyldigeVirkningstidspunkt(): List<Arguments> =
            listOf(
                Arguments.of("0826", YearMonth.of(2026, 8)),
                Arguments.of("0170", YearMonth.of(1970, 1)),
                Arguments.of("0136", YearMonth.of(1936, 1)),
                Arguments.of("1235", YearMonth.of(2035, 12)),
                Arguments.of("1269", YearMonth.of(1969, 12)),
                Arguments.of("1299", YearMonth.of(1999, 12)),
                Arguments.of(" 0826 ", YearMonth.of(2026, 8)),
                Arguments.of("", null),
                Arguments.of("0000", null),
            )
    }
}
