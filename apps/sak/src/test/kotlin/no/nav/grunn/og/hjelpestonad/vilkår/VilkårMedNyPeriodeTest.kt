package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.DiagnoseRegler
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class VilkårMedNyPeriodeTest {
    private val behandlingId = UUID.randomUUID()

    private fun dato(verdi: String?) = verdi?.let(LocalDate::parse)

    private fun medlemskap(
        fra: String? = null,
        til: String? = null,
        id: UUID = UUID.randomUUID(),
        begrunnelse: String = "Eksisterende",
    ) = VilkårMedlemskap(
        id = id,
        behandlingId = behandlingId,
        regelverk = Regelverk.EØS_FORORDNINGEN,
        vurdering = Vurdering.NEI,
        begrunnelse = begrunnelse,
        fraOgMedDato = dato(fra),
        tilOgMedDato = dato(til),
    )

    private fun nyMedlemskap(
        fra: String? = null,
        til: String? = null,
        id: UUID = UUID.randomUUID(),
    ) = VilkårMedlemskap(
        id = id,
        behandlingId = behandlingId,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = Vurdering.JA,
        begrunnelse = "Ny",
        fraOgMedDato = dato(fra),
        tilOgMedDato = dato(til),
    )

    private fun List<VilkårMedlemskap>.medDatoer(
        fra: String?,
        til: String?,
    ) = single { it.fraOgMedDato == dato(fra) && it.tilOgMedDato == dato(til) }

    @Test
    fun `lar periode stå urørt når den nye ikke overlapper`() {
        val lagret = medlemskap("2025-01-01", "2025-06-30")
        val ny = nyMedlemskap("2025-07-01", "2025-12-31")

        val resultat = listOf(lagret).medNyPeriode(ny)

        assertThat(resultat).containsExactly(lagret, ny)
        assertThat(resultat.first().id).isEqualTo(lagret.id)
    }

    @Test
    fun `forkorter lagret periode bakfra`() {
        val lagret = medlemskap("2025-01-01", "2025-06-30")
        val ny = nyMedlemskap("2025-03-01", "2025-12-31")

        val resultat = listOf(lagret).medNyPeriode(ny)

        val rest = resultat.medDatoer("2025-01-01", "2025-02-28")
        assertThat(rest.id).isNotEqualTo(lagret.id)
        assertThat(rest.behandlingId).isEqualTo(lagret.behandlingId)
        assertThat(resultat).contains(ny)
    }

    @Test
    fun `forkorter lagret periode forfra`() {
        val lagret = medlemskap("2025-01-01", "2025-06-30")
        val ny = nyMedlemskap("2024-01-01", "2025-02-28")

        val resultat = listOf(lagret).medNyPeriode(ny)

        val rest = resultat.medDatoer("2025-03-01", "2025-06-30")
        assertThat(rest.id).isNotEqualTo(lagret.id)
        assertThat(resultat).contains(ny)
    }

    @Test
    fun `forkorter lagret periode med én dag når periodene møtes samme dag`() {
        val lagret = medlemskap("2025-01-01", "2025-06-30")
        val ny = nyMedlemskap("2025-06-30", "2025-12-31")

        val resultat = listOf(lagret).medNyPeriode(ny)

        val rest = resultat.medDatoer("2025-01-01", "2025-06-29")
        assertThat(rest.id).isNotEqualTo(lagret.id)
        assertThat(resultat).contains(ny)
    }

    @Test
    fun `splitter lagret periode i to med nye ulike id-er`() {
        val lagret = medlemskap("2025-01-01", "2025-12-31")
        val ny = nyMedlemskap("2025-06-01", "2025-06-30")

        val resultat = listOf(lagret).medNyPeriode(ny)
        val førsteRest = resultat.medDatoer("2025-01-01", "2025-05-31")
        val andreRest = resultat.medDatoer("2025-07-01", "2025-12-31")

        assertThat(førsteRest.id).isNotEqualTo(lagret.id)
        assertThat(andreRest.id).isNotEqualTo(lagret.id).isNotEqualTo(førsteRest.id)
        assertThat(resultat).contains(ny).doesNotContain(lagret)
    }

    @Test
    fun `beholder åpne ender når en løpende periode splittes`() {
        val lagret = medlemskap()
        val ny = nyMedlemskap("2025-01-01", "2025-12-31")

        val resultat = listOf(lagret).medNyPeriode(ny)

        val førsteRest = resultat.medDatoer(null, "2024-12-31")
        val andreRest = resultat.medDatoer("2026-01-01", null)
        assertThat(førsteRest.id).isNotEqualTo(lagret.id)
        assertThat(andreRest.id).isNotEqualTo(lagret.id).isNotEqualTo(førsteRest.id)
        assertThat(resultat).contains(ny).doesNotContain(lagret)
    }

    @Test
    fun `fjerner lagret periode som den nye dekker helt`() {
        val lagret = medlemskap("2025-03-01", "2025-05-31")
        val ny = nyMedlemskap("2025-01-01", "2025-12-31")

        val resultat = listOf(lagret).medNyPeriode(ny)

        assertThat(resultat).containsExactly(ny).doesNotContain(lagret)
    }

    @Test
    fun `løpende ny periode uten datoer fjerner alle lagrede perioder`() {
        val første = medlemskap("2025-01-01", "2025-06-30")
        val andre = medlemskap("2026-01-01", "2026-06-30")
        val ny = nyMedlemskap()

        val resultat = listOf(første, andre).medNyPeriode(ny)

        assertThat(resultat).containsExactly(ny).doesNotContain(første, andre)
    }

    @Test
    fun `behandler hver lagret periode for seg og beholder id på urørt periode`() {
        val urørt = medlemskap("2024-01-01", "2024-06-30")
        val forkortet = medlemskap("2025-01-01", "2025-12-31")
        val dekket = medlemskap("2026-01-01", "2026-03-31")
        val ny = nyMedlemskap("2025-07-01", "2026-06-30")

        val resultat = listOf(urørt, forkortet, dekket).medNyPeriode(ny)

        assertThat(resultat).contains(urørt, ny)
        assertThat(resultat.medDatoer("2024-01-01", "2024-06-30").id).isEqualTo(urørt.id)
        assertThat(resultat.medDatoer("2025-01-01", "2025-06-30").id).isNotEqualTo(forkortet.id)
        assertThat(resultat).doesNotContain(forkortet, dekket)
    }

    @Test
    fun `beholder vilkårets egne felt og sporbar når perioden forkortes`() {
        val lagret = medlemskap("2025-01-01", "2025-06-30")
        val ny = nyMedlemskap("2025-03-01", "2025-12-31")

        val rest = listOf(lagret).medNyPeriode(ny).medDatoer("2025-01-01", "2025-02-28")

        assertThat(rest.id).isNotEqualTo(lagret.id)
        assertThat(rest.behandlingId).isEqualTo(lagret.behandlingId)
        assertThat(rest.regelverk).isEqualTo(lagret.regelverk)
        assertThat(rest.vurdering).isEqualTo(lagret.vurdering)
        assertThat(rest.begrunnelse).isEqualTo(lagret.begrunnelse)
        assertThat(rest.sporbar).isEqualTo(lagret.sporbar)
    }

    @Test
    fun `endring erstatter perioden med samme id og forkorter naboer`() {
        val endret = medlemskap("2025-01-01", "2025-03-31")
        val nabo = medlemskap("2025-04-01", "2025-12-31")
        val nyEndret = nyMedlemskap("2025-01-01", "2025-06-30", id = endret.id)

        val resultat = listOf(endret, nabo).medNyPeriode(nyEndret)

        assertThat(resultat).contains(nyEndret).doesNotContain(endret, nabo)
        val rest = resultat.medDatoer("2025-07-01", "2025-12-31")
        assertThat(rest.id).isNotEqualTo(nabo.id)
        assertThat(resultat).hasSize(2)
    }

    @Test
    fun `datoer i ny periode valideres`() {
        assertThatThrownBy {
            listOf(medlemskap()).medNyPeriode(nyMedlemskap("2025-06-01", "2025-01-01"))
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `datoer i ny periode valideres også når listen er tom`() {
        assertThatThrownBy {
            emptyList<VilkårMedlemskap>().medNyPeriode(nyMedlemskap("2025-06-01", "2025-01-01"))
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `datoer i ny diagnoseperiode valideres når lagrede perioder har en annen diagnosekode`() {
        val annenKode = diagnose("2025-01-01", "2025-12-31", kode = "K900")
        val ugyldigNyPeriode = diagnose("2025-06-01", "2025-01-01", kode = "H833")

        assertThatThrownBy { listOf(annenKode).medNyPeriode(ugyldigNyPeriode) }
            .isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `medlemskapsperioder med ulikt regelverk forkorter hverandre på samme tidslinje`() {
        val lagretNasjonal = medlemskap("2025-01-01", "2025-06-30")
        val lagretEøs = medlemskap("2025-07-01", "2025-12-31").copy(regelverk = Regelverk.EØS_FORORDNINGEN)
        val ny = nyMedlemskap("2025-05-01", "2025-08-31")

        val resultat = listOf(lagretNasjonal, lagretEøs).medNyPeriode(ny)

        assertThat(resultat).contains(ny)
        assertThat(resultat).anySatisfy {
            assertThat(it.fraOgMedDato).isEqualTo(dato("2025-01-01"))
            assertThat(it.tilOgMedDato).isEqualTo(dato("2025-04-30"))
        }
        assertThat(resultat).anySatisfy {
            assertThat(it.fraOgMedDato).isEqualTo(dato("2025-09-01"))
            assertThat(it.tilOgMedDato).isEqualTo(dato("2025-12-31"))
        }
        assertThat(resultat).doesNotContain(lagretNasjonal, lagretEøs)
    }

    @Test
    fun `diagnoseperioder med ulik kode overlapper uten å endres`() {
        val lagret = diagnose("2025-01-01", "2025-12-31", kode = "H833")
        val ny = diagnose("2025-06-01", "2025-06-30", kode = "K900")

        val resultat = lagreDiagnose(listOf(lagret), ny)

        assertThat(resultat).containsExactly(lagret, ny)
        assertThat(resultat.first().id).isEqualTo(lagret.id)
        assertThat(resultat.last().id).isEqualTo(ny.id)
    }

    @Test
    fun `diagnoseperioder med samme kode forkorter hverandre`() {
        val lagret = diagnose("2025-01-01", "2025-12-31")
        val ny = diagnose("2025-06-01", "2025-06-30")

        val resultat = lagreDiagnose(listOf(lagret), ny)

        val førsteRest = resultat.single { it.fraOgMedDato == dato("2025-01-01") }
        val andreRest = resultat.single { it.fraOgMedDato == dato("2025-07-01") }
        assertThat(førsteRest.tilOgMedDato).isEqualTo(dato("2025-05-31"))
        assertThat(andreRest.tilOgMedDato).isEqualTo(dato("2025-12-31"))
        assertThat(førsteRest.id).isNotEqualTo(lagret.id)
        assertThat(andreRest.id).isNotEqualTo(lagret.id).isNotEqualTo(førsteRest.id)
        assertThat(resultat).contains(ny).doesNotContain(lagret)
    }

    @Test
    fun `urørt periode i overlappgruppe beholder id`() {
        val urørt = medlemskap("2024-01-01", "2024-06-30")
        val overlappende = medlemskap("2025-01-01", "2025-12-31")
        val ny = nyMedlemskap("2025-06-01", "2025-06-30")

        val resultat = listOf(urørt, overlappende).medNyPeriode(ny)

        assertThat(resultat).contains(urørt)
        assertThat(resultat.medDatoer("2024-01-01", "2024-06-30").id).isEqualTo(urørt.id)
        assertThat(resultat).doesNotContain(overlappende)
    }

    private fun diagnose(
        fra: String,
        til: String,
        vurdering: Vurdering = Vurdering.JA,
        yrkesskade: Boolean = false,
        kode: String = "H833",
        id: UUID = UUID.randomUUID(),
    ) = VilkårDiagnose(
        id = id,
        behandlingId = behandlingId,
        kode = kode,
        tekst = "Tekst",
        vurdering = vurdering,
        erYrkesskade = yrkesskade,
        fraOgMedDato = dato(fra),
        tilOgMedDato = dato(til),
    )

    private fun lagreDiagnose(
        lagrede: List<VilkårDiagnose>,
        ny: VilkårDiagnose,
    ): List<VilkårDiagnose> =
        lagrede
            .medNyPeriode(ny)
            .also(DiagnoseRegler::validerMaksEnYrkesskadePeriode)

    @Test
    fun `avviser startoverlapp med oppfylt yrkesskade uten å nevne diagnose`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)

        assertThatThrownBy { lagreDiagnose(listOf(yrkesskade), diagnose("2020-01-01", "2020-06-30")) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage(DiagnoseRegler.YRKESSKADE_FORDELT_PÅ_FLERE_PERIODER)
            .hasMessageNotContaining("H833")
    }

    @Test
    fun `avviser NEI-periode midt i oppfylt yrkesskade når den gir en splitt`() {
        val yrkesskade = diagnose("2020-01-01", "2020-12-31", yrkesskade = true)

        assertThatThrownBy {
            lagreDiagnose(listOf(yrkesskade), diagnose("2020-06-01", "2020-06-30", vurdering = Vurdering.NEI))
        }.isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `godtar NEI-periode over starten av yrkesskade og beholder markeringen`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)

        val resultat = lagreDiagnose(listOf(yrkesskade), diagnose("2020-01-01", "2020-06-30", vurdering = Vurdering.NEI))

        val rest = resultat.single { it.fraOgMedDato == dato("2020-07-01") }
        assertThat(rest.tilOgMedDato).isEqualTo(dato("2020-12-31"))
        assertThat(rest.id).isNotEqualTo(yrkesskade.id)
        assertThat(rest.erYrkesskade).isTrue()
    }

    @Test
    fun `godtar NEI-periode over slutten av yrkesskade`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)

        val resultat = lagreDiagnose(listOf(yrkesskade), diagnose("2020-01-01", "2021-01-31", vurdering = Vurdering.NEI))

        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().fraOgMedDato).isEqualTo(dato("2020-01-01"))
        assertThat(resultat.single().tilOgMedDato).isEqualTo(dato("2021-01-31"))
        assertThat(resultat.single().vurdering).isEqualTo(Vurdering.NEI)
    }

    @Test
    fun `godtar oppfylt periode som dekker hele yrkesskaden`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)
        val ny = diagnose("2020-01-01", "2021-01-31")

        val resultat = lagreDiagnose(listOf(yrkesskade), ny)

        assertThat(resultat).containsExactly(ny).doesNotContain(yrkesskade)
    }

    @Test
    fun `avviser ny yrkesskade ved overlappende oppfylt periode med samme kode`() {
        val annenPeriode = diagnose("2020-03-01", "2020-12-31")

        assertThatThrownBy {
            lagreDiagnose(listOf(annenPeriode), diagnose("2020-01-01", "2020-06-30", yrkesskade = true))
        }.isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `godtar endring av selve yrkesskaden`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)
        val endret = diagnose("2020-06-01", "2020-12-31", yrkesskade = true, id = yrkesskade.id)

        val resultat = lagreDiagnose(listOf(yrkesskade), endret)

        assertThat(resultat).containsExactly(endret)
        assertThat(resultat.single().id).isEqualTo(yrkesskade.id)
    }

    @Test
    fun `godtar overlappende oppfylt periode med en annen diagnosekode`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)
        val annenKode = diagnose("2020-01-01", "2020-06-30", kode = "K900")

        val resultat = lagreDiagnose(listOf(yrkesskade), annenKode)

        assertThat(resultat).containsExactly(yrkesskade, annenKode)
    }

    @Test
    fun `avviser oppfylt periode med samme diagnosekode etter yrkesskaden`() {
        val yrkesskade = diagnose("2020-03-01", "2020-12-31", yrkesskade = true)

        assertThatThrownBy { lagreDiagnose(listOf(yrkesskade), diagnose("2021-01-01", "2021-12-31")) }
            .isInstanceOf(UgyldigInput::class.java)
    }
}
