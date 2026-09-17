package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRequest
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
    ) = VilkårMedlemskap(
        id = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = Vurdering.JA,
        fraOgMedDato = fraOgMedDato?.let { LocalDate.parse(it) },
        tilOgMedDato = tilOgMedDato?.let { LocalDate.parse(it) },
    )

    private fun request(
        fraOgMedDato: String? = null,
        tilOgMedDato: String? = null,
    ) = VilkårMedlemskapRequest(
        regelverk = Regelverk.NASJONALE_REGLER,
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
    fun `tom liste gir en tom tidslinje`() {
        assertThat(emptyList<VilkårMedlemskap>().tilTidslinje().erTom()).isTrue()
    }

    @Test
    fun `godtar periode som ligger inntil en lagret periode`() {
        assertThatCode {
            listOf(periode("2025-01-01", "2025-06-30")).validerIngenOverlappMed(request("2025-07-01", "2025-12-31"))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `godtar periode på én enkelt dag`() {
        assertThatCode { emptyList<VilkårMedlemskap>().validerIngenOverlappMed(request("2025-01-01", "2025-01-01")) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `godtar løpende periode når det ikke finnes lagrede perioder`() {
        assertThatCode { emptyList<VilkårMedlemskap>().validerIngenOverlappMed(request()) }.doesNotThrowAnyException()
    }

    @Test
    fun `avviser periode som overlapper en lagret periode`() {
        assertThatThrownBy {
            listOf(periode("2025-01-01", "2025-06-30")).validerIngenOverlappMed(request("2025-03-01", "2025-12-31"))
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `avviser perioder som møtes på samme dag`() {
        assertThatThrownBy {
            listOf(periode("2025-01-01", "2025-06-30")).validerIngenOverlappMed(request("2025-06-30", "2025-12-31"))
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `avviser to løpende perioder fordi begge dekker hele tidslinjen`() {
        assertThatThrownBy { listOf(periode()).validerIngenOverlappMed(request()) }
            .isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `avviser løpende periode som overlapper en senere lagret periode`() {
        assertThatThrownBy {
            listOf(periode("2026-01-01", "2026-02-01")).validerIngenOverlappMed(request(fraOgMedDato = "2025-01-01"))
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `avviser til og med-dato før fra og med-dato`() {
        assertThatThrownBy { emptyList<VilkårMedlemskap>().validerIngenOverlappMed(request("2025-06-01", "2025-01-01")) }
            .isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }
}
