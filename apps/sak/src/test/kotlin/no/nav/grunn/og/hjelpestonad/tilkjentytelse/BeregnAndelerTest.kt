package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertFailsWith

class BeregnAndelerTest {
    private val behandlingId = UUID.randomUUID()
    private val beregningsmåned = YearMonth.of(2026, 11)

    @Test
    fun `manglende sluttdato fylles med tre år frem uten avrunding`() {
        val andel = fyllInnManglendeSluttdato(listOf(UtledetAndel(YearMonth.of(2026, 4), null, "829.125".toBigDecimal())), beregningsmåned, behandlingId).single()
        assertThat(andel.fom).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(andel.tom).isEqualTo(LocalDate.of(2029, 11, 30))
        assertThat(andel.beløp).isEqualByComparingTo("829.125")
        assertThat(andel.kildeBehandlingId).isEqualTo(behandlingId)
    }

    @Test
    fun `eksplisitte sluttdatoer beholdes også for perioder som starter mer enn tre år frem`() {
        val andeler =
            fyllInnManglendeSluttdato(
                listOf(
                    UtledetAndel(YearMonth.of(2025, 1), YearMonth.of(2025, 6), 752.toBigDecimal()),
                    UtledetAndel(YearMonth.of(2029, 12), YearMonth.of(2035, 1), 1146.toBigDecimal()),
                ),
                beregningsmåned,
                behandlingId,
            )
        assertThat(andeler.map { it.tom }).containsExactly(LocalDate.of(2025, 6, 30), LocalDate.of(2035, 1, 31))
    }

    @Test
    fun `åpen periode etter standard sluttdato avvises fremfor å forsvinne fra beregningen`() {
        assertFailsWith<UgyldigInput> {
            fyllInnManglendeSluttdato(
                listOf(UtledetAndel(YearMonth.of(2029, 12), null, 1146.toBigDecimal())),
                beregningsmåned,
                behandlingId,
            )
        }
    }
}
