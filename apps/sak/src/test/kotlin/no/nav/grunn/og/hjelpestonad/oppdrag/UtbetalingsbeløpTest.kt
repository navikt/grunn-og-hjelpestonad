package no.nav.grunn.og.hjelpestonad.oppdrag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UtbetalingsbeløpTest {
    @Test
    fun `nøyaktig 50 øre rundes opp til neste hele krone`() {
        assertThat(tilUtbetalingsbeløp(BigDecimal("286.50"))).isEqualTo(287)
    }

    @Test
    fun `under 50 øre rundes ned til forrige hele krone`() {
        assertThat(tilUtbetalingsbeløp(BigDecimal("286.49"))).isEqualTo(286)
    }

    @Test
    fun `hele kroner endres ikke`() {
        assertThat(tilUtbetalingsbeløp(BigDecimal("1000"))).isEqualTo(1000)
    }

    @Test
    fun `tre desimaler under 50 øre rundes ned`() {
        assertThat(tilUtbetalingsbeløp(BigDecimal("286.125"))).isEqualTo(286)
    }
}
