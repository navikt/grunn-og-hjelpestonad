package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.erOppfyltTidslinje
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

/**
 * Medlemskapsvilkåret sett sammen med diagnosene, jf. ftrl. § 6-9 om yrkesskade.
 */
class MedlemskapOgDiagnoseTidslinjeTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `diagnose og medlemskap i samme periode gir oppfylt periode`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-12-31"))
                .erOppfyltTidslinje(listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")))

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `diagnose uten medlemskap gir ingen oppfylt periode`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-12-31", vurdering = Vurdering.NEI))
                .erOppfyltTidslinje(listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")))

        assertThat(tidslinje.oppfyltePerioder()).isEmpty()
    }

    @Test
    fun `medlemskap uten diagnose gir ingen oppfylt periode`() {
        val tidslinje = listOf(medlemskap("2025-01-01", "2025-12-31")).erOppfyltTidslinje(emptyList())

        assertThat(tidslinje.oppfyltePerioder()).isEmpty()
    }

    @Test
    fun `diagnose som ikke er oppfylt teller ikke`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-12-31"))
                .erOppfyltTidslinje(listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", vurdering = Vurdering.NEI)))

        assertThat(tidslinje.oppfyltePerioder()).isEmpty()
    }

    @Test
    fun `oppfylt bare i overlappet mellom diagnose og medlemskap`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-06-30"))
                .erOppfyltTidslinje(listOf(diagnose("Diabetes type 1", "2025-04-01", "2025-12-31")))

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 4, 1) to LocalDate.of(2025, 6, 30),
        )
    }

    @Test
    fun `minst én diagnose holder`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-12-31"))
                .erOppfyltTidslinje(
                    listOf(
                        diagnose("Diabetes type 1", "2025-01-01", "2025-06-30"),
                        diagnose("Støyskade", "2025-07-01", "2025-12-31"),
                    ),
                )

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `yrkesskade med medlemskap ved start gir rett ut hele diagnoseperioden`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-03-31"))
                .erOppfyltTidslinje(listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", erYrkesskade = true)))

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `yrkesskade uten medlemskap ved start gir ingen lemping`() {
        val tidslinje =
            listOf(medlemskap("2025-07-01", "2025-12-31"))
                .erOppfyltTidslinje(listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", erYrkesskade = true)))

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 7, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `yrkesskade med medlemskap vurdert til NEI ved start gir ingen lemping`() {
        val tidslinje =
            listOf(
                medlemskap("2025-01-01", "2025-03-31", vurdering = Vurdering.NEI),
                medlemskap("2025-04-01", "2025-12-31"),
            ).erOppfyltTidslinje(listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", erYrkesskade = true)))

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 4, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `yrkesskade uten fra og med-dato kaster`() {
        assertThatThrownBy {
            listOf(medlemskap(null, "2025-03-31"))
                .erOppfyltTidslinje(listOf(diagnose("Støyskade", null, "2025-12-31", erYrkesskade = true)))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("fra og med")
    }

    @Test
    fun `yrkesskade lemper bare medlemskapskravet for sin egen diagnoseperiode`() {
        val tidslinje =
            listOf(medlemskap("2025-01-01", "2025-01-31"))
                .erOppfyltTidslinje(
                    listOf(
                        diagnose("Støyskade", "2025-01-01", "2025-06-30", erYrkesskade = true),
                        diagnose("Diabetes type 1", "2025-07-01", "2025-12-31"),
                    ),
                )

        assertThat(tidslinje.oppfyltePerioder()).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 6, 30),
        )
    }

    @Test
    fun `yrkesskade fordelt på flere perioder støttes ikke enda`() {
        assertThatThrownBy {
            listOf(medlemskap("2020-01-01", "2020-12-31"))
                .erOppfyltTidslinje(
                    listOf(
                        diagnose("Støyskade", "2020-01-01", "2020-12-31", erYrkesskade = true),
                        diagnose(" støyskade ", "2023-01-01", "2023-12-31", erYrkesskade = true),
                    ),
                )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("støttes ikke enda")
            .hasMessageNotContaining("øyskade")
    }

    @Test
    fun `tomt grunnlag gir tom tidslinje`() {
        assertThat(emptyList<VilkårMedlemskap>().erOppfyltTidslinje(emptyList()).erTom()).isTrue()
    }

    private fun Tidslinje<Boolean>.oppfyltePerioder(): List<Pair<LocalDate?, LocalDate?>> =
        slåSammenLikePerioder()
            .tilPerioder()
            .filtrerIkkeNull()
            .filter { it.verdi }
            .map { it.fom to it.tom }

    private fun medlemskap(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårMedlemskap(
        behandlingId = behandlingId,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = vurdering,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )

    private fun diagnose(
        diagnose: String,
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        vurdering: Vurdering = Vurdering.JA,
        erYrkesskade: Boolean = false,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        diagnose = diagnose,
        erYrkesskade = erYrkesskade,
        vurdering = vurdering,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )
}
