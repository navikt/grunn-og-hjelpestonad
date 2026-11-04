package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class SatsbeløpTest {
    @ParameterizedTest(name = "sats {0}")
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `periodene for hver sats starter 1997, har verken overlapp eller hull og slutter med en åpen periode`(sats: Int) {
        val perioder = Satsbeløpene.alle.filter { it.sats == sats }.sortedBy { it.gyldigFraOgMed }

        assertThat(perioder.first().gyldigFraOgMed).isEqualTo(LocalDate.of(1997, 1, 1))
        assertThat(perioder.last().gyldigTilOgMed).isNull()
        perioder.zipWithNext { forrige, neste ->
            assertThat(forrige.gyldigTilOgMed)
                .describedAs("sats $sats: perioden fra ${forrige.gyldigFraOgMed} skal slutte dagen før ${neste.gyldigFraOgMed}")
                .isEqualTo(neste.gyldigFraOgMed.minusDays(1))
        }
    }

    @Test
    fun `listen har bare sats 1 til 6`() {
        assertThat(Satsbeløpene.alle.map { it.sats }.toSet()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `sats 1 i 1997 er 480 kroner i måneden`() {
        assertThat(Satsbeløpene.hent(sats = 1, dato = LocalDate.of(1997, 1, 1))).isEqualTo(480)
    }

    @Test
    fun `oppslag før 1997 kaster feil i stedet for å bruke nærmeste beløp`() {
        assertThatThrownBy { Satsbeløpene.hent(sats = 1, dato = LocalDate.of(1996, 12, 31)) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `oppslag for en sats som ikke finnes kaster feil`() {
        assertThatThrownBy { Satsbeløpene.hent(sats = 7, dato = LocalDate.of(2024, 1, 1)) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    /** Forventede beløp er skrevet av fra rundskrivet til § 6-3, https://lovdata.no/nav/rundskriv/v1-06-03 */
    @ParameterizedTest(name = "sats {0} den {1} er {2} kroner")
    @CsvSource(
        "1, 1997-01-01, 480",
        "6, 1997-12-31, 2400",
        "1, 1998-01-01, 491",
        "3, 2001-06-15, 1028",
        "4, 2004-07-01, 1611",
        "2, 2012-05-01, 968",
        "1, 2023-01-01, 705",
        "6, 2023-06-30, 3517",
        "1, 2023-07-01, 724",
        "6, 2023-12-31, 3606",
        "1, 2024-01-01, 752",
        "6, 2024-01-01, 3744",
        "5, 2026-11-04, 2997",
    )
    fun `satsbeløpet stemmer med rundskrivet`(
        sats: Int,
        dato: LocalDate,
        månedsbeløp: Int,
    ) {
        assertThat(Satsbeløpene.hent(sats = sats, dato = dato)).isEqualTo(månedsbeløp)
    }
}
