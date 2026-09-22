package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRequest
import org.assertj.core.api.Assertions.assertThat
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
    fun `fellesfunksjonen lager tidslinje for alle vilkårstypene`() {
        val medlemskap =
            VilkårMedlemskap(
                behandlingId = UUID.randomUUID(),
                regelverk = Regelverk.NASJONALE_REGLER,
                vurdering = Vurdering.JA,
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            )
        val diagnose =
            VilkårDiagnose(
                behandlingId = UUID.randomUUID(),
                vurdering = Vurdering.JA,
                diagnose = "Diabetes type 1",
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            )
        val institusjon =
            VilkårInstitusjon(
                behandlingId = UUID.randomUUID(),
                vurdering = Vurdering.NEI,
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            )

        assertThat(listOf(medlemskap).tilTidslinje().tilPerioder().map { it.verdi }).containsExactly(medlemskap)
        assertThat(listOf(diagnose).tilTidslinje().tilPerioder().map { it.verdi }).containsExactly(diagnose)
        assertThat(listOf(institusjon).tilTidslinje().tilPerioder().map { it.verdi }).containsExactly(institusjon)

        assertThat(listOf(medlemskap).erOppfyltTidslinje().tilPerioder().map { it.verdi }).containsExactly(true)
        assertThat(listOf(diagnose).erOppfyltTidslinje().tilPerioder().map { it.verdi }).containsExactly(true)
        assertThat(listOf(institusjon).erOppfyltTidslinje().tilPerioder().map { it.verdi }).containsExactly(false)
    }

    @Test
    fun `tom tidslinje er tom`() {
        assertThat(tomTidslinje<Boolean>().erTom()).isTrue()
    }

    @Test
    fun `lar periode som ligger inntil en lagret periode stå urørt`() {
        val lagret = periode("2025-01-01", "2025-06-30")

        val gjenværende = listOf(lagret).forkortetAv(request("2025-07-01", "2025-12-31"))

        assertThat(gjenværende).hasSize(1)
        assertThat(gjenværende[0].verdi).isEqualTo(lagret)
        assertThat(gjenværende[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2025, 6, 30))
    }

    @Test
    fun `forkorter lagret periode bakfra når den nye perioden starter inni den`() {
        val gjenværende = listOf(periode("2025-01-01", "2025-06-30")).forkortetAv(request("2025-03-01", "2025-12-31"))

        assertThat(gjenværende).hasSize(1)
        assertThat(gjenværende[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun `forkorter lagret periode forfra når den nye perioden slutter inni den`() {
        val gjenværende = listOf(periode("2025-01-01", "2025-06-30")).forkortetAv(request("2024-01-01", "2025-02-28"))

        assertThat(gjenværende).hasSize(1)
        assertThat(gjenværende[0].fom).isEqualTo(LocalDate.of(2025, 3, 1))
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2025, 6, 30))
    }

    @Test
    fun `forkorter lagret periode med én dag når periodene møtes på samme dag`() {
        val gjenværende = listOf(periode("2025-01-01", "2025-06-30")).forkortetAv(request("2025-06-30", "2025-12-31"))

        assertThat(gjenværende).hasSize(1)
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2025, 6, 29))
    }

    @Test
    fun `splitter lagret periode i to når den nye perioden ligger midt inni`() {
        val lagret = periode("2025-01-01", "2025-12-31")

        val gjenværende = listOf(lagret).forkortetAv(request("2025-06-01", "2025-06-30"))

        assertThat(gjenværende).hasSize(2)
        assertThat(gjenværende.map { it.verdi }).containsOnly(lagret)
        assertThat(gjenværende[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2025, 5, 31))
        assertThat(gjenværende[1].fom).isEqualTo(LocalDate.of(2025, 7, 1))
        assertThat(gjenværende[1].tom).isEqualTo(LocalDate.of(2025, 12, 31))
    }

    @Test
    fun `splitter løpende lagret periode og beholder de åpne endene`() {
        val gjenværende = listOf(periode()).forkortetAv(request("2025-01-01", "2025-12-31"))

        assertThat(gjenværende).hasSize(2)
        assertThat(gjenværende[0].fom).isNull()
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(gjenværende[1].fom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(gjenværende[1].tom).isNull()
    }

    @Test
    fun `fjerner lagret periode som den nye perioden dekker i sin helhet`() {
        assertThat(listOf(periode("2025-03-01", "2025-05-31")).forkortetAv(request("2025-01-01", "2025-12-31"))).isEmpty()
    }

    @Test
    fun `fjerner alle lagrede perioder når den nye perioden er løpende uten datoer`() {
        assertThat(listOf(periode("2025-01-01", "2025-06-30"), periode("2026-01-01", "2026-06-30")).forkortetAv(request()))
            .isEmpty()
    }

    @Test
    fun `rører bare de lagrede periodene som faktisk overlapper`() {
        val urørt = periode("2024-01-01", "2024-12-31")
        val overlappet = periode("2025-01-01", "2025-12-31")

        val gjenværende = listOf(urørt, overlappet).forkortetAv(request("2025-07-01", "2026-12-31"))

        assertThat(gjenværende).hasSize(2)
        assertThat(gjenværende[0].verdi).isEqualTo(urørt)
        assertThat(gjenværende[0].tom).isEqualTo(LocalDate.of(2024, 12, 31))
        assertThat(gjenværende[1].verdi).isEqualTo(overlappet)
        assertThat(gjenværende[1].tom).isEqualTo(LocalDate.of(2025, 6, 30))
    }

    @Test
    fun `gir ingen tidsrom tilbake når det ikke finnes lagrede perioder`() {
        assertThat(emptyList<VilkårMedlemskap>().forkortetAv(request("2025-01-01", "2025-01-01"))).isEmpty()
        assertThat(emptyList<VilkårMedlemskap>().forkortetAv(request())).isEmpty()
    }

    @Test
    fun `avviser til og med-dato før fra og med-dato`() {
        assertThatThrownBy { emptyList<VilkårMedlemskap>().forkortetAv(request("2025-06-01", "2025-01-01")) }
            .isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }
}
