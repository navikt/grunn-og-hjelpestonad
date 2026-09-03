package no.nav.gjenlevende.bs.sak.fagsak

import io.mockk.mockk
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class SøkControllerTest {
    private val søkController = SøkController(søkService = mockk())

    @Test
    fun `validerErPersonident ident med 11 siffer er innafor`() {
        val gyldigPersonident = "12345678901"
        søkController.validerErPersonident(gyldigPersonident)
    }

    @Test
    fun `validerErPersonident ident med mindre enn 11 siffer skal gi feilmeding`() {
        val ugyldigPersonident = "123"

        val exception =
            assertThrows<Feil> {
                søkController.validerErPersonident(ugyldigPersonident)
            }

        assertEquals("Personident må være 11 siffer", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.httpStatus)
    }

    @Test
    fun `validerErPersonident med ident som inneholder bokstaver skal gi feilmeding`() {
        val ugyldigPersonident = "123456789AB"

        val exception =
            assertThrows<Feil> {
                søkController.validerErPersonident(ugyldigPersonident)
            }

        assertEquals("Personident kan kun inneholde tall", exception.message)
        assertEquals(HttpStatus.BAD_REQUEST, exception.httpStatus)
    }
}
