package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PerioderMedOppfylteInngangsvilkårUtleder
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VurderteInngangsvilkår
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

private const val DIABETES_TYPE_1 = "E109"
private const val STØYSKADE = "H833"

/**
 * Sannhetstabellen for perioder med oppfylte inngangsvilkår. Ren test uten Spring og uten mockk,
 * jf. ADR-0006 og ADR-0008.
 */
class PerioderMedOppfylteInngangsvilkårTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `begge inngangsvilkår oppfylt i samme periode gir én periode`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser = listOf(diagnose(DIABETES_TYPE_1, "2025-01-01", "2025-12-31")),
            )

        assertThat(perioder).hasSize(1)
        assertThat(perioder.single().fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(perioder.single().tilOgMedDato).isEqualTo(LocalDate.of(2025, 12, 31))
    }

    @Test
    fun `ett inngangsvilkår NEI i midten gir to perioder`() {
        val perioder =
            utled(
                medlemskap =
                    listOf(
                        medlemskap("2025-01-01", "2025-03-31"),
                        medlemskap("2025-04-01", "2025-05-31", vurdering = Vurdering.NEI),
                        medlemskap("2025-06-01", "2025-12-31"),
                    ),
                diagnoser = listOf(diagnose(DIABETES_TYPE_1, "2025-01-01", "2025-12-31")),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31),
            LocalDate.of(2025, 6, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `hull i ett inngangsvilkår gir ingen periode i hullet`() {
        val perioder =
            utled(
                medlemskap =
                    listOf(
                        medlemskap("2025-01-01", "2025-03-31"),
                        medlemskap("2025-06-01", "2025-12-31"),
                    ),
                diagnoser = listOf(diagnose(DIABETES_TYPE_1, "2025-01-01", "2025-12-31")),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31),
            LocalDate.of(2025, 6, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `åpen fra og med og åpen til og med bevares som null`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap(null, null)),
                diagnoser = listOf(diagnose(DIABETES_TYPE_1, null, null)),
            )

        assertThat(perioder).hasSize(1)
        assertThat(perioder.single().fraOgMedDato).isNull()
        assertThat(perioder.single().tilOgMedDato).isNull()
    }

    @Test
    fun `to ulike diagnoser som overlapper kaster ikke, og gir periode når minst én er JA`() {
        val vurderteInngangsvilkår =
            vurderteInngangsvilkår(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser =
                    listOf(
                        diagnose(DIABETES_TYPE_1, "2025-01-01", "2025-06-30"),
                        diagnose(STØYSKADE, "2025-01-01", "2025-12-31", vurdering = Vurdering.NEI),
                    ),
            )

        assertThatCode { PerioderMedOppfylteInngangsvilkårUtleder.utled(vurderteInngangsvilkår) }.doesNotThrowAnyException()

        assertThat(PerioderMedOppfylteInngangsvilkårUtleder.utled(vurderteInngangsvilkår).map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 6, 30),
        )
    }

    @Test
    fun `samme diagnose to ganger etter hverandre slås sammen til én periode`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser =
                    listOf(
                        diagnose(DIABETES_TYPE_1, "2025-01-01", "2025-06-30"),
                        diagnose(DIABETES_TYPE_1, "2025-07-01", "2025-12-31"),
                    ),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `ingen vurderte perioder gir ingen perioder, fordi et tidsrom uten vurdering regnes som nei`() {
        assertThat(utled()).isEmpty()
    }

    @Test
    fun `inngangsvilkår uten overlappende periode gir ingen perioder`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-03-31")),
                diagnoser = listOf(diagnose(DIABETES_TYPE_1, "2025-04-01", "2025-06-30")),
            )

        assertThat(perioder).isEmpty()
    }

    @Test
    fun `yrkesskade lemper medlemskapskravet når medlemskapet var oppfylt da diagnosen startet`() {
        val perioder =
            utled(
                medlemskap =
                    listOf(
                        medlemskap("2025-01-01", "2025-03-31"),
                        medlemskap("2025-04-01", "2025-12-31", vurdering = Vurdering.NEI),
                    ),
                diagnoser = listOf(diagnose(STØYSKADE, "2025-01-01", "2025-12-31", erYrkesskade = true)),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `yrkesskade uten medlemskap ved diagnosens start lemper ikke medlemskapskravet`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31", vurdering = Vurdering.NEI)),
                diagnoser = listOf(diagnose(STØYSKADE, "2025-01-01", "2025-12-31", erYrkesskade = true)),
            )

        assertThat(perioder).isEmpty()
    }

    private fun utled(
        medlemskap: List<VilkårMedlemskap> = emptyList(),
        diagnoser: List<VilkårDiagnose> = emptyList(),
    ) = PerioderMedOppfylteInngangsvilkårUtleder.utled(vurderteInngangsvilkår(medlemskap, diagnoser))

    private fun vurderteInngangsvilkår(
        medlemskap: List<VilkårMedlemskap> = emptyList(),
        diagnoser: List<VilkårDiagnose> = emptyList(),
    ) = VurderteInngangsvilkår(
        behandlingId = behandlingId,
        medlemskap = medlemskap,
        diagnoser = diagnoser,
    )

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
        kode: String,
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        vurdering: Vurdering = Vurdering.JA,
        erYrkesskade: Boolean = false,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = kode,
        tekst = "Tekst fra kodeverket",
        erYrkesskade = erYrkesskade,
        vurdering = vurdering,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )
}
