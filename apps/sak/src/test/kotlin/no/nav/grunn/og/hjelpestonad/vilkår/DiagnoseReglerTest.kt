package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.DiagnoseRegler
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class DiagnoseReglerTest {
    private fun request(
        kode: String,
        erYrkesskade: Boolean = false,
        fraOgMedDato: LocalDate? = null,
    ) = VilkårDiagnoseRequest(
        kode = kode,
        erYrkesskade = erYrkesskade,
        vurdering = Vurdering.JA,
        fraOgMedDato = fraOgMedDato,
    )

    @Test
    fun `avviser tom diagnosekode`() {
        assertThatThrownBy { DiagnoseRegler.valider(request("   ")) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Diagnosekode kan ikke være tom")
    }

    @Test
    fun `avviser ukjent diagnosekode uten å røpe koden`() {
        assertThatThrownBy { DiagnoseRegler.valider(request("X999")) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Diagnosekoden finnes ikke i ICD-10")
            .hasMessageNotContaining("X999")
    }

    @Test
    fun `avviser kode fra ICPC-2`() {
        assertThatThrownBy { DiagnoseRegler.valider(request("T89")) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Diagnosekoden finnes ikke i ICD-10")
    }

    @Test
    fun `avviser yrkesskade uten fra og med-dato`() {
        assertThatThrownBy { DiagnoseRegler.valider(request("E109", erYrkesskade = true)) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("En yrkesskade må ha en fra og med-dato")
    }

    @Test
    fun `godtar gyldig diagnosekode og yrkesskade med dato`() {
        assertThatCode {
            DiagnoseRegler.valider(
                request(" e10.9 ", erYrkesskade = true, fraOgMedDato = LocalDate.of(2025, 1, 1)),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `diagnoseperioder med samme kode har lik tidslinjenøkkel`() {
        val diabetes = diagnose("E109")
        val diabetes2 = diagnose("E109")

        assertThat(diabetes.tidslinjenøkkel()).isEqualTo(diabetes2.tidslinjenøkkel())
    }

    @Test
    fun `diagnoseperioder med ulik kode har ulik tidslinjenøkkel`() {
        val diabetes = diagnose("E109")
        val cøliaki = diagnose("K900")

        assertThat(diabetes.tidslinjenøkkel()).isNotEqualTo(cøliaki.tidslinjenøkkel())
    }

    private fun diagnose(kode: String) =
        VilkårDiagnose(
            behandlingId = UUID.randomUUID(),
            kode = kode,
            tekst = "Tekst",
            vurdering = Vurdering.JA,
        )

    @Test
    fun `avviser to oppfylte yrkesskadeperioder med samme kode uten å røpe koden`() {
        assertThatThrownBy {
            DiagnoseRegler.validerMaksEnYrkesskadePeriode(listOf(diagnose("H833").copy(erYrkesskade = true), diagnose("H833")))
        }.isInstanceOf(UgyldigInput::class.java)
            .hasMessage(DiagnoseRegler.YRKESSKADE_FORDELT_PÅ_FLERE_PERIODER)
            .hasMessageNotContaining("H833")
    }

    @Test
    fun `NEI-periode teller ikke sammen med oppfylt yrkesskade`() {
        assertThatCode {
            DiagnoseRegler.validerMaksEnYrkesskadePeriode(
                listOf(diagnose("H833").copy(erYrkesskade = true), diagnose("H833").copy(vurdering = Vurdering.NEI)),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `to oppfylte perioder uten yrkesskade med samme kode godtas`() {
        assertThatCode { DiagnoseRegler.validerMaksEnYrkesskadePeriode(listOf(diagnose("H833"), diagnose("H833"))) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `oppfylt yrkesskade og oppfylt periode med annen kode godtas`() {
        assertThatCode {
            DiagnoseRegler.validerMaksEnYrkesskadePeriode(listOf(diagnose("H833").copy(erYrkesskade = true), diagnose("K900")))
        }.doesNotThrowAnyException()
    }
}
