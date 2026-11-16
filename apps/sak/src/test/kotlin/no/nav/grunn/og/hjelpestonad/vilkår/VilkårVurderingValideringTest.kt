package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingValidering
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VurderteInngangsvilkår
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
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
        val feil = VilkårVurderingValidering.valider(alleInngangsvilkårVurdert())

        assertThat(feil).isEmpty()
    }

    @Test
    fun `R1 - manglende medlemskapsvurdering gir feil på medlemskapsvilkåret`() {
        val feil = VilkårVurderingValidering.valider(alleInngangsvilkårVurdert().copy(medlemskap = emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactly(VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN)
    }

    @Test
    fun `R1 - manglende diagnosevurdering gir feil på diagnosevilkåret`() {
        val feil = VilkårVurderingValidering.valider(alleInngangsvilkårVurdert().copy(diagnoser = emptyList()))

        assertThat(feil.map { it.vilkårType }).containsExactly(VilkårType.DIAGNOSE)
    }

    @Test
    fun `R1 - ingen vurderte perioder gir én feil per inngangsvilkår`() {
        val feil = VilkårVurderingValidering.valider(VurderteInngangsvilkår(behandlingId, emptyList(), emptyList()))

        assertThat(feil.map { it.vilkårType })
            .containsExactlyInAnyOrder(VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN, VilkårType.DIAGNOSE)
        assertThat(feil.map { it.melding }).doesNotContainNull()
    }

    @Test
    fun `R3 - manglende begrunnelse ved NEI er ikke en feil`() {
        val vurderteInngangsvilkår =
            alleInngangsvilkårVurdert().copy(
                diagnoser = listOf(diagnose(vurdering = Vurdering.NEI, begrunnelse = "")),
            )

        assertThat(VilkårVurderingValidering.valider(vurderteInngangsvilkår)).isEmpty()
    }

    @Test
    fun `R4 - hull mellom vilkårsperiodene er ikke en feil`() {
        val vurderteInngangsvilkår =
            alleInngangsvilkårVurdert().copy(
                medlemskap =
                    listOf(
                        medlemskap(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31)),
                        medlemskap(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31)),
                    ),
            )

        assertThat(VilkårVurderingValidering.valider(vurderteInngangsvilkår)).isEmpty()
    }

    @Test
    fun `alle vilkår vurdert til NEI er ikke en feil, men et avslagsløp`() {
        val vurderteInngangsvilkår =
            VurderteInngangsvilkår(
                behandlingId = behandlingId,
                medlemskap = listOf(medlemskap(vurdering = Vurdering.NEI)),
                diagnoser = listOf(diagnose(vurdering = Vurdering.NEI)),
            )

        assertThat(VilkårVurderingValidering.valider(vurderteInngangsvilkår)).isEmpty()
    }

    @Test
    fun `feilmeldingene røper ikke diagnoseopplysninger`() {
        val feil = VilkårVurderingValidering.valider(VurderteInngangsvilkår(behandlingId, emptyList(), emptyList()))

        assertThat(feil.map { it.melding }).noneMatch { it.contains("Diabetes", ignoreCase = true) || it.contains("E109") }
    }

    private fun alleInngangsvilkårVurdert() =
        VurderteInngangsvilkår(
            behandlingId = behandlingId,
            medlemskap = listOf(medlemskap()),
            diagnoser = listOf(diagnose()),
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

    private fun diagnose(
        vurdering: Vurdering = Vurdering.JA,
        begrunnelse: String = "Diagnosen er dokumentert",
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = "E109",
        tekst = "Diabetes mellitus type 1 uten komplikasjoner",
        vurdering = vurdering,
        begrunnelse = begrunnelse,
    )
}
