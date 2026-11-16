package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BegrunnelseValideringTest {
    @Test
    fun `tom begrunnelse er tillatt`() {
        validerBegrunnelse("")
    }

    @Test
    fun `2000 tegn er tillatt`() {
        validerBegrunnelse("ø".repeat(2000))
    }

    @Test
    fun `2001 tegn avvises uten at begrunnelsen tas med i feilmeldingen`() {
        val begrunnelse = "ø".repeat(2001)

        val feil = assertFailsWith<UgyldigInput> { validerBegrunnelse(begrunnelse) }

        assertThat(feil.melding).isEqualTo("Begrunnelse kan ikke være lengre enn 2000 tegn.")
        assertThat(feil.melding).doesNotContain(begrunnelse)
    }

    @Test
    fun `lengden telles i UTF-16 som i frontend`() {
        validerBegrunnelse("😀".repeat(1000))

        assertFailsWith<UgyldigInput> { validerBegrunnelse("😀".repeat(1000) + "a") }
    }
}
