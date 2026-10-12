package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PersonidentValideringTest {
    @Test
    fun `ident med 11 siffer er gyldig`() {
        assertDoesNotThrow { validerErPersonident("12345678901") }
    }

    @Test
    fun `tom ident gir feilmelding`() {
        val exception = assertThrows<UgyldigInput> { validerErPersonident(" ") }

        assertEquals("Personident kan ikke være tom", exception.melding)
    }

    @Test
    fun `ident med mindre enn 11 siffer gir feilmelding`() {
        val exception = assertThrows<UgyldigInput> { validerErPersonident("123") }

        assertEquals("Personident må være 11 siffer", exception.melding)
    }

    @Test
    fun `ident med bokstaver gir feilmelding`() {
        val exception = assertThrows<UgyldigInput> { validerErPersonident("123456789AB") }

        assertEquals("Personident kan kun inneholde tall", exception.melding)
    }
}
