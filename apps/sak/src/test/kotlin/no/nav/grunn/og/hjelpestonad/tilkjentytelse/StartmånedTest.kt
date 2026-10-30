package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class StartmånedTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `førstegangsbehandling får måneden til første andel`() {
        val andeler = setOf(andel("2027-03", "2027-12"), andel("2028-01", "2028-06"))

        val startmåned = utledStartmåned(andeler = andeler, forrigeStartmåned = null, opphørsmåned = null)

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `revurdering med andel før forrige startmåned flytter startmåneden bakover`() {
        val andeler = setOf(andel("2027-01", "2027-12"))

        val startmåned = utledStartmåned(andeler = andeler, forrigeStartmåned = YearMonth.of(2027, 3), opphørsmåned = null)

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 1))
    }

    @Test
    fun `revurdering med første andel etter forrige startmåned beholder forrige startmåned`() {
        val andeler = setOf(andel("2027-06", "2027-12"))

        val startmåned = utledStartmåned(andeler = andeler, forrigeStartmåned = YearMonth.of(2027, 3), opphørsmåned = null)

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `opphør uten andeler etter forrige startmåned beholder forrige startmåned`() {
        val startmåned =
            utledStartmåned(
                andeler = emptySet(),
                forrigeStartmåned = YearMonth.of(2027, 3),
                opphørsmåned = YearMonth.of(2027, 8),
            )

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `opphør uten andeler før forrige startmåned gir opphørsmåneden`() {
        val startmåned =
            utledStartmåned(
                andeler = emptySet(),
                forrigeStartmåned = YearMonth.of(2027, 3),
                opphørsmåned = YearMonth.of(2027, 1),
            )

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 1))
    }

    @Test
    fun `uten andeler og uten opphørsmåned beholdes forrige startmåned`() {
        val startmåned = utledStartmåned(andeler = emptySet(), forrigeStartmåned = YearMonth.of(2027, 3), opphørsmåned = null)

        assertThat(startmåned).isEqualTo(YearMonth.of(2027, 3))
    }

    @Test
    fun `uten andeler, forrige startmåned og opphørsmåned er ugyldig`() {
        assertThatThrownBy { utledStartmåned(andeler = emptySet(), forrigeStartmåned = null, opphørsmåned = null) }
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
