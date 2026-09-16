package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class VilkårTidslinjeTest {
    private fun periode(
        fraOgMedDato: String? = null,
        tilOgMedDato: String? = null,
        vilkårType: VilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE,
    ) = VilkårVurdering(
        id = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        vilkårType = vilkårType,
        vurdering = Vurdering.JA,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )

    @Test
    fun `legger periodene på tidslinjen i kronologisk rekkefølge`() {
        val andre = periode("2025-07-01", "2025-12-31")
        val første = periode("2025-01-01", "2025-06-30")

        val tidslinje = listOf(andre, første).tilTidslinje()

        assertThat(tidslinje.tilPerioder().map { it.verdi }).containsExactly(første, andre)
    }

    @Test
    fun `beholder åpen fra og med og åpen til og med som uendelige perioder`() {
        val tidslinje = listOf(periode(tilOgMedDato = "2025-01-31"), periode(fraOgMedDato = "2025-02-01")).tilTidslinje()

        val perioder = tidslinje.tilPerioder()
        assertThat(perioder.first().fom).isNull()
        assertThat(perioder.last().tom).isNull()
    }

    @Test
    fun `validerer innkommende request mot lagrede vurderinger`() {
        val lagret = periode("2025-01-01", "2025-06-30")
        val request =
            VilkårVurderingRequest(
                vilkårType = lagret.vilkårType,
                vurdering = Vurdering.JA,
                fraOgMedDato = LocalDate.of(2025, 7, 1),
                tilOgMedDato = LocalDate.of(2025, 12, 31),
            )

        assertThatCode { listOf(lagret).validerIngenOverlappMed(request) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser innkommende request som overlapper en lagret vurdering`() {
        val lagret = periode("2025-01-01", "2025-06-30")
        val request =
            VilkårVurderingRequest(
                vilkårType = lagret.vilkårType,
                vurdering = Vurdering.JA,
                fraOgMedDato = LocalDate.of(2025, 3, 1),
                tilOgMedDato = LocalDate.of(2025, 12, 31),
            )

        assertThatThrownBy { listOf(lagret).validerIngenOverlappMed(request) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `tillater perioder som ligger inntil hverandre`() {
        assertThatCode {
            listOf(periode("2025-01-01", "2025-06-30"), periode("2025-07-01", "2025-12-31")).tilTidslinje()
        }.doesNotThrowAnyException()
    }

    @Test
    fun `tillater periode på én enkelt dag`() {
        assertThatCode { listOf(periode("2025-01-01", "2025-01-01")).tilTidslinje() }.doesNotThrowAnyException()
    }

    @Test
    fun `tom liste gir en tom tidslinje`() {
        assertThat(emptyList<VilkårVurdering>().tilTidslinje().erTom()).isTrue()
    }

    @Test
    fun `avviser overlappende perioder`() {
        assertThatThrownBy {
            listOf(periode("2025-01-01", "2025-06-30"), periode("2025-03-01", "2025-12-31")).tilTidslinje()
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `avviser perioder som møtes på samme dag`() {
        assertThatThrownBy {
            listOf(periode("2025-01-01", "2025-06-30"), periode("2025-06-30", "2025-12-31")).tilTidslinje()
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `avviser to perioder uten datoer fordi begge dekker hele tidslinjen`() {
        assertThatThrownBy { listOf(periode(), periode()).tilTidslinje() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `avviser løpende periode som overlapper en senere periode`() {
        assertThatThrownBy {
            listOf(periode(fraOgMedDato = "2025-01-01"), periode("2026-01-01", "2026-02-01")).tilTidslinje()
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `avviser til og med-dato før fra og med-dato`() {
        assertThatThrownBy { listOf(periode("2025-06-01", "2025-01-01")).tilTidslinje() }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
