package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Diagnosekode
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Icd10
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class Icd10Test {
    @Test
    fun `finn normaliserer punktum, mellomrom og små bokstaver`() {
        val diagnosekode = Icd10.finn(" e10.9 ")

        assertThat(diagnosekode).isEqualTo(Diagnosekode("E109", "Diabetes mellitus type 1 uten komplikasjoner"))
    }

    @Test
    fun `finn gir ingen treff på kode fra et annet kodeverk`() {
        // T89 er diabetes type 1 i ICPC-2.
        assertThat(Icd10.finn("T89")).isNull()
    }

    @Test
    fun `søk på kode gir eksakt treff først og deretter koder som starter likt`() {
        assertThat(Icd10.søk("e10").map { it.kode })
            .isNotEmpty
            .allMatch { it.startsWith("E10") }
        assertThat(Icd10.søk("E10.9").first().kode).isEqualTo("E109")
    }

    @Test
    fun `søk på tekst krever at alle ordene finnes, uavhengig av rekkefølge og store bokstaver`() {
        val treff = Icd10.søk("TYPE 1 diabetes")

        assertThat(treff)
            .isNotEmpty
            .allMatch { it.tekst.contains("diabetes", ignoreCase = true) && it.tekst.contains("type 1", ignoreCase = true) }
        assertThat(treff.map { it.kode }).contains("E109")
    }

    @Test
    fun `søk rangerer tekst som starter med søkeordet foran tekst som bare inneholder det`() {
        val starterMedSøkeordet = Icd10.søk("psoriasis").map { it.tekst.startsWith("psoriasis", ignoreCase = true) }

        assertThat(starterMedSøkeordet).contains(true, false)
        assertThat(starterMedSøkeordet).isEqualTo(starterMedSøkeordet.sortedDescending())
    }

    @Test
    fun `søk begrenser antall treff`() {
        assertThat(Icd10.søk("a")).hasSize(Icd10.MAKS_ANTALL_TREFF)
    }

    @Test
    fun `tomt søk gir ingen treff`() {
        assertThat(Icd10.søk("   ")).isEmpty()
    }
}
