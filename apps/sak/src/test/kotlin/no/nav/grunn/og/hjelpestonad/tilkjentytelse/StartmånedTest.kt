package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test

/**
 * 🔴 Rød sone: testskjelett for [utledStartmåned]. Reglene følger `startdato` i EF,
 * se ADR-0010. Testene feiler til funksjonen er skrevet.
 */
class StartmånedTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `førstegangsbehandling får måneden til første andel`() {
        val andeler = setOf(andel("2027-03", "2027-12"), andel("2028-01", "2028-06"))

        assertThat(utledStartmåned(andeler = andeler, forrigeStartmåned = null))
            .isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `revurdering med andel før forrige startmåned flytter startmåneden bakover`() {
        val andeler = setOf(andel("2027-01", "2027-12"))

        assertThat(utledStartmåned(andeler = andeler, forrigeStartmåned = YearMonth.of(2027, 3)))
            .isEqualTo(YearMonth.of(2027, 1))
    }

    @Test
    fun `revurdering med første andel etter forrige startmåned beholder forrige startmåned`() {
        val andeler = setOf(andel("2027-06", "2027-12"))

        assertThat(utledStartmåned(andeler = andeler, forrigeStartmåned = YearMonth.of(2027, 3)))
            .isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `revurdering som opphører alle andeler beholder forrige startmåned`() {
        assertThat(utledStartmåned(andeler = emptySet(), forrigeStartmåned = YearMonth.of(2027, 3)))
            .isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `førstegangsbehandling uten andeler er ugyldig`() {
        // TODO 🔴: Bekreft regelen. Kan en førstegangsbehandling få tilkjent ytelse uten
        // andeler? I dag lager TilkjentYtelseService ingen tilkjent ytelse ved avslag.
        assertThatThrownBy { utledStartmåned(andeler = emptySet(), forrigeStartmåned = null) }
            .isInstanceOf(UgyldigInput::class.java)
    }

    private fun andel(
        fom: String,
        tom: String,
    ) = AndelTilkjentYtelse(
        beløp = 1000,
        fom = YearMonth.parse(fom).atDay(1),
        tom = YearMonth.parse(tom).atEndOfMonth(),
        kildeBehandlingId = behandlingId,
    )
}
