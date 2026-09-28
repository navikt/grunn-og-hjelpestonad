package no.nav.grunn.og.hjelpestonad.infotrygd.util

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InfotrygdFormatTest {
    @Test
    fun `reverserFnr går begge veier`() {
        assertEquals("70040312345", "03047012345".reverserFnr())
        assertEquals("03047012345", "70040312345".reverserFnr())
    }

    @Test
    fun `reverserFnr avviser ugyldige identer`() {
        assertFailsWith<IllegalArgumentException> { "0304701234".reverserFnr() }
        assertFailsWith<IllegalArgumentException> { "0304701234a".reverserFnr() }
    }

    @Test
    fun `datoFraSaTabell leser DDMMÅÅÅÅ og gir null for 0 og ugyldige datoer`() {
        assertEquals(LocalDate.of(2019, 1, 1), datoFraSaTabell(1012019))
        assertEquals(LocalDate.of(2019, 12, 31), datoFraSaTabell(31122019))
        assertNull(datoFraSaTabell(0))
        assertNull(datoFraSaTabell(32132019))
    }
}
