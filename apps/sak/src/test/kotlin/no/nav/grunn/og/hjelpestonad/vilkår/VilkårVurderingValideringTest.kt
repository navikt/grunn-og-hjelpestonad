package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingStegGrunnlag
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingValidering
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

/**
 * Én test per regel i ADR-0004. Ren test uten Spring og uten mockk.
 */
class VilkårVurderingValideringTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `komplett vilkårsvurdering gir ingen feil`() {
        val feil = VilkårVurderingValidering.valider(komplettGrunnlag())

        assertThat(feil).isEmpty()
    }

    @Test
    fun `R1 - manglende medlemskapsvurdering gir feil på medlemskapsvilkåret`() {
        val feil = VilkårVurderingValidering.valider(komplettGrunnlag().copy(medlemskap = emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactly(VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN)
    }

    @Test
    fun `R1 - manglende diagnosevurdering gir feil på diagnosevilkåret`() {
        val feil = VilkårVurderingValidering.valider(komplettGrunnlag().copy(diagnoser = emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactly(VilkårType.DIAGNOSE)
    }

    @Test
    fun `R1 - manglende institusjonsvurdering gir feil på institusjonsvilkåret`() {
        val feil = VilkårVurderingValidering.valider(komplettGrunnlag().copy(institusjon = emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactly(VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM)
    }

    @Test
    fun `R1 - tomt grunnlag gir én feil per vilkår`() {
        val feil = VilkårVurderingValidering.valider(VilkårVurderingStegGrunnlag(behandlingId, emptyList(), emptyList(), emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactlyInAnyOrder(*VilkårType.entries.toTypedArray())
        assertThat(feil.map { it.melding }).doesNotContainNull()
    }

    @Test
    fun `R3 - manglende begrunnelse ved NEI er ikke en feil`() {
        val grunnlag =
            komplettGrunnlag().copy(
                institusjon = listOf(institusjon(vurdering = Vurdering.NEI, begrunnelse = "")),
            )

        assertThat(VilkårVurderingValidering.valider(grunnlag)).isEmpty()
    }

    @Test
    fun `R4 - hull mellom vilkårsperiodene er ikke en feil`() {
        val grunnlag =
            komplettGrunnlag().copy(
                medlemskap =
                    listOf(
                        medlemskap(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)),
                        medlemskap(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31)),
                    ),
            )

        assertThat(VilkårVurderingValidering.valider(grunnlag)).isEmpty()
    }

    @Test
    fun `alle vilkår vurdert til NEI er ikke en feil, men et avslagsløp`() {
        val grunnlag =
            VilkårVurderingStegGrunnlag(
                behandlingId = behandlingId,
                medlemskap = listOf(medlemskap(vurdering = Vurdering.NEI)),
                diagnoser = listOf(diagnose(vurdering = Vurdering.NEI)),
                institusjon = listOf(institusjon(vurdering = Vurdering.NEI)),
            )

        assertThat(VilkårVurderingValidering.valider(grunnlag)).isEmpty()
    }

    @Test
    fun `feilmeldingene røper ikke diagnoseopplysninger`() {
        val feil = VilkårVurderingValidering.valider(VilkårVurderingStegGrunnlag(behandlingId, emptyList(), emptyList(), emptyList()))

        assertThat(feil.map { it.melding }).noneMatch { it.contains("Diabetes", ignoreCase = true) }
    }

    private fun komplettGrunnlag() =
        VilkårVurderingStegGrunnlag(
            behandlingId = behandlingId,
            medlemskap = listOf(medlemskap()),
            diagnoser = listOf(diagnose()),
            institusjon = listOf(institusjon()),
        )

    private fun medlemskap(
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårMedlemskap(
        behandlingId = behandlingId,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = vurdering,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun diagnose(vurdering: Vurdering = Vurdering.JA) =
        VilkårDiagnose(
            behandlingId = behandlingId,
            diagnose = "Diabetes type 1",
            vurdering = vurdering,
        )

    private fun institusjon(
        vurdering: Vurdering = Vurdering.JA,
        begrunnelse: String = "Bor hjemme",
    ) = VilkårInstitusjon(
        behandlingId = behandlingId,
        vurdering = vurdering,
        begrunnelse = begrunnelse,
    )
}
