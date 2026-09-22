package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PeriodeMedRettUtleder
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingStegGrunnlag
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.Oppholdstype
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

/**
 * Sannhetstabellen for utledningen. Ren test uten Spring og uten mockk, jf. ADR-0004.
 */
class PeriodeMedRettUtlederTest {
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `alle vilkår oppfylt i samme periode gir én periode med rett`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser = listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
            )

        assertThat(perioder).hasSize(1)
        assertThat(perioder.single().fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(perioder.single().tilOgMedDato).isEqualTo(LocalDate.of(2025, 12, 31))
        assertThat(perioder.single().behandlingId).isEqualTo(behandlingId)
    }

    @Test
    fun `ett vilkår NEI i midten gir to perioder med rett`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser = listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")),
                institusjon =
                    listOf(
                        institusjon("2025-01-01", "2025-03-31"),
                        institusjon("2025-04-01", "2025-05-31", vurdering = Vurdering.NEI),
                        institusjon("2025-06-01", "2025-12-31"),
                    ),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31),
            LocalDate.of(2025, 6, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `hull i ett vilkår gir ingen rett i hullet`() {
        val perioder =
            utled(
                medlemskap =
                    listOf(
                        medlemskap("2025-01-01", "2025-03-31"),
                        medlemskap("2025-06-01", "2025-12-31"),
                    ),
                diagnoser = listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
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
                diagnoser = listOf(diagnose("Diabetes type 1", null, null)),
                institusjon = listOf(institusjon(null, null)),
            )

        assertThat(perioder).hasSize(1)
        assertThat(perioder.single().fraOgMedDato).isNull()
        assertThat(perioder.single().tilOgMedDato).isNull()
    }

    @Test
    fun `to ulike diagnoser som overlapper kaster ikke, og gir rett når minst én er JA`() {
        val grunnlag =
            grunnlag(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser =
                    listOf(
                        diagnose("Diabetes type 1", "2025-01-01", "2025-06-30"),
                        diagnose("Støyskade", "2025-01-01", "2025-12-31", vurdering = Vurdering.NEI),
                    ),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
            )

        assertThatCode { PeriodeMedRettUtleder.utled(grunnlag) }.doesNotThrowAnyException()

        assertThat(PeriodeMedRettUtleder.utled(grunnlag).map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 6, 30),
        )
    }

    @Test
    fun `samme diagnose to ganger etter hverandre slås sammen til én periode med rett`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser =
                    listOf(
                        diagnose("Diabetes type 1", "2025-01-01", "2025-06-30"),
                        diagnose(" diabetes TYPE 1 ", "2025-07-01", "2025-12-31"),
                    ),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
            )

        assertThat(perioder.map { it.fraOgMedDato to it.tilOgMedDato }).containsExactly(
            LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31),
        )
    }

    @Test
    fun `opphold i institusjon uten unntak gir ingen rett i perioden`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-12-31")),
                diagnoser = listOf(diagnose("Diabetes type 1", "2025-01-01", "2025-12-31")),
                institusjon =
                    listOf(
                        institusjon(
                            "2025-01-01",
                            "2025-12-31",
                            vurdering = Vurdering.NEI,
                            oppholdstype = Oppholdstype.HELSE_OG_OMSORGSINSTITUSJON,
                        ),
                    ),
            )

        assertThat(perioder).isEmpty()
    }

    @Test
    fun `tomt grunnlag kaster VilkårValideringFeil i stedet for å gi tom liste`() {
        assertThatThrownBy { PeriodeMedRettUtleder.utled(grunnlag()) }
            .isInstanceOf(VilkårValideringFeil::class.java)
            .extracting { (it as VilkårValideringFeil).feil.map { feil -> feil.vilkårType } }
            .isEqualTo(
                listOf(
                    VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN,
                    VilkårType.DIAGNOSE,
                    VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM,
                ),
            )
    }

    @Test
    fun `vilkår uten overlappende periode gir ingen rett`() {
        val perioder =
            utled(
                medlemskap = listOf(medlemskap("2025-01-01", "2025-03-31")),
                diagnoser = listOf(diagnose("Diabetes type 1", "2025-04-01", "2025-06-30")),
                institusjon = listOf(institusjon(null, null)),
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
                diagnoser = listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", erYrkesskade = true)),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
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
                diagnoser = listOf(diagnose("Støyskade", "2025-01-01", "2025-12-31", erYrkesskade = true)),
                institusjon = listOf(institusjon("2025-01-01", "2025-12-31")),
            )

        assertThat(perioder).isEmpty()
    }

    private fun utled(
        medlemskap: List<VilkårMedlemskap> = emptyList(),
        diagnoser: List<VilkårDiagnose> = emptyList(),
        institusjon: List<VilkårInstitusjon> = emptyList(),
    ) = PeriodeMedRettUtleder.utled(grunnlag(medlemskap, diagnoser, institusjon))

    private fun grunnlag(
        medlemskap: List<VilkårMedlemskap> = emptyList(),
        diagnoser: List<VilkårDiagnose> = emptyList(),
        institusjon: List<VilkårInstitusjon> = emptyList(),
    ) = VilkårVurderingStegGrunnlag(
        behandlingId = behandlingId,
        medlemskap = medlemskap,
        diagnoser = diagnoser,
        institusjon = institusjon,
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

    private fun institusjon(
        fraOgMedDato: String?,
        tilOgMedDato: String?,
        vurdering: Vurdering = Vurdering.JA,
        oppholdstype: Oppholdstype? = null,
    ) = VilkårInstitusjon(
        behandlingId = behandlingId,
        vurdering = vurdering,
        oppholdstype = oppholdstype,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )
}
