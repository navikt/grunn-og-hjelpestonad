package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BeregnAndelerTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `manglende sluttdato beholdes uten avrunding`() {
        val andel = lagAndelerTilkjentYtelse(listOf(UtledetAndel(YearMonth.of(2026, 4), null, "829.125".toBigDecimal())), behandlingId).single()
        assertThat(andel.fom).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(andel.tom).isNull()
        assertThat(andel.beløp).isEqualByComparingTo("829.125")
        assertThat(andel.kildeBehandlingId).isEqualTo(behandlingId)
    }

    @Test
    fun `eksplisitte sluttdatoer beholdes også for perioder som starter mer enn tre år frem`() {
        val andeler =
            lagAndelerTilkjentYtelse(
                listOf(
                    UtledetAndel(YearMonth.of(2025, 1), YearMonth.of(2025, 6), 752.toBigDecimal()),
                    UtledetAndel(YearMonth.of(2029, 12), YearMonth.of(2035, 1), 1146.toBigDecimal()),
                ),
                behandlingId,
            )
        assertThat(andeler.map { it.tom }).containsExactly(LocalDate.of(2025, 6, 30), LocalDate.of(2035, 1, 31))
    }

    @Test
    fun `åpen periode langt frem i tid beholdes`() {
        val andeler =
            lagAndelerTilkjentYtelse(
                listOf(UtledetAndel(YearMonth.of(2029, 12), null, 1146.toBigDecimal())),
                behandlingId,
            )
        assertThat(andeler.single().fom).isEqualTo(LocalDate.of(2029, 12, 1))
        assertThat(andeler.single().tom).isNull()
    }
}
