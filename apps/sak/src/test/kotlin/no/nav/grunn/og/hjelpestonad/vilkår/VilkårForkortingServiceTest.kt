package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRepository
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRequest
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

class VilkårForkortingServiceTest {
    private val repository = mockk<VilkårMedlemskapRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service = VilkårMedlemskapService(repository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    private val behandlingId = UUID.randomUUID()
    private val innsatte = mutableListOf<VilkårMedlemskap>()

    init {
        every { repository.insert(capture(innsatte)) } answers { firstArg() }
        every { repository.update(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    private fun dato(dato: String?) = dato?.let { LocalDate.parse(it) }

    private fun lagret(
        navn: String,
        fraOgMedDato: String? = null,
        tilOgMedDato: String? = null,
    ) = VilkårMedlemskap(
        behandlingId = behandlingId,
        regelverk = Regelverk.EØS_FORORDNINGEN,
        vurdering = Vurdering.NEI,
        begrunnelse = navn,
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun nyPeriode(
        fraOgMedDato: String? = null,
        tilOgMedDato: String? = null,
        id: UUID? = null,
    ) = VilkårMedlemskapRequest(
        id = id,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = Vurdering.JA,
        begrunnelse = "Ny",
        fraOgMedDato = dato(fraOgMedDato),
        tilOgMedDato = dato(tilOgMedDato),
    )

    private fun lagre(vararg lagredePerioder: VilkårMedlemskap): (VilkårMedlemskapRequest) -> Unit {
        every { repository.findByBehandlingId(behandlingId) } returns lagredePerioder.toList()
        return { request -> service.lagrePeriode(behandlingId, request) }
    }

    /** Bitene den lagrede perioden [navn] ble skrevet tilbake som, i kronologisk rekkefølge. */
    private fun biterAv(navn: String) = innsatte.filter { it.begrunnelse == navn }

    @Test
    fun `lar lagret periode stå urørt når den nye ikke overlapper`() {
        val urørt = lagret("Urørt", "2025-01-01", "2025-06-30")

        lagre(urørt)(nyPeriode("2025-07-01", "2025-12-31"))

        verify(exactly = 0) { repository.deleteById(any()) }
        assertThat(innsatte.map { it.begrunnelse }).containsExactly("Ny")
    }

    @Test
    fun `forkorter lagret periode bakfra når den nye starter inni den`() {
        val overlappet = lagret("Overlappet", "2025-01-01", "2025-06-30")

        lagre(overlappet)(nyPeriode("2025-03-01", "2025-12-31"))

        verify(exactly = 1) { repository.deleteById(overlappet.id) }
        assertThat(biterAv("Overlappet")).hasSize(1)
        assertThat(biterAv("Overlappet")[0].fraOgMedDato).isEqualTo(dato("2025-01-01"))
        assertThat(biterAv("Overlappet")[0].tilOgMedDato).isEqualTo(dato("2025-02-28"))
    }

    @Test
    fun `forkorter lagret periode forfra når den nye slutter inni den`() {
        val overlappet = lagret("Overlappet", "2025-01-01", "2025-06-30")

        lagre(overlappet)(nyPeriode("2024-01-01", "2025-02-28"))

        assertThat(biterAv("Overlappet")).hasSize(1)
        assertThat(biterAv("Overlappet")[0].fraOgMedDato).isEqualTo(dato("2025-03-01"))
        assertThat(biterAv("Overlappet")[0].tilOgMedDato).isEqualTo(dato("2025-06-30"))
    }

    @Test
    fun `forkorter lagret periode med én dag når periodene møtes på samme dag`() {
        val overlappet = lagret("Overlappet", "2025-01-01", "2025-06-30")

        lagre(overlappet)(nyPeriode("2025-06-30", "2025-12-31"))

        assertThat(biterAv("Overlappet")).hasSize(1)
        assertThat(biterAv("Overlappet")[0].tilOgMedDato).isEqualTo(dato("2025-06-29"))
    }

    @Test
    fun `splitter lagret periode i to når den nye ligger midt inni`() {
        val splittet = lagret("Splittet", "2025-01-01", "2025-12-31")

        lagre(splittet)(nyPeriode("2025-06-01", "2025-06-30"))

        verify(exactly = 1) { repository.deleteById(splittet.id) }
        val biter = biterAv("Splittet")
        assertThat(biter).hasSize(2)
        assertThat(biter[0].fraOgMedDato).isEqualTo(dato("2025-01-01"))
        assertThat(biter[0].tilOgMedDato).isEqualTo(dato("2025-05-31"))
        assertThat(biter[1].fraOgMedDato).isEqualTo(dato("2025-07-01"))
        assertThat(biter[1].tilOgMedDato).isEqualTo(dato("2025-12-31"))
    }

    @Test
    fun `gir hver bit av en splittet periode sin egen id`() {
        val splittet = lagret("Splittet", "2025-01-01", "2025-12-31")

        lagre(splittet)(nyPeriode("2025-06-01", "2025-06-30"))

        assertThat(biterAv("Splittet").map { it.id }).doesNotHaveDuplicates().doesNotContain(splittet.id)
    }

    @Test
    fun `beholder de åpne endene når en løpende lagret periode splittes`() {
        lagre(lagret("Løpende"))(nyPeriode("2025-01-01", "2025-12-31"))

        val biter = biterAv("Løpende")
        assertThat(biter).hasSize(2)
        assertThat(biter[0].fraOgMedDato).isNull()
        assertThat(biter[0].tilOgMedDato).isEqualTo(dato("2024-12-31"))
        assertThat(biter[1].fraOgMedDato).isEqualTo(dato("2026-01-01"))
        assertThat(biter[1].tilOgMedDato).isNull()
    }

    @Test
    fun `sletter lagret periode som den nye dekker i sin helhet`() {
        val dekket = lagret("Dekket", "2025-03-01", "2025-05-31")

        lagre(dekket)(nyPeriode("2025-01-01", "2025-12-31"))

        verify(exactly = 1) { repository.deleteById(dekket.id) }
        assertThat(biterAv("Dekket")).isEmpty()
    }

    @Test
    fun `sletter alle lagrede perioder når den nye er løpende uten datoer`() {
        val første = lagret("Første", "2025-01-01", "2025-06-30")
        val andre = lagret("Andre", "2026-01-01", "2026-06-30")

        lagre(første, andre)(nyPeriode())

        verify(exactly = 1) { repository.deleteById(første.id) }
        verify(exactly = 1) { repository.deleteById(andre.id) }
        assertThat(innsatte.map { it.begrunnelse }).containsExactly("Ny")
    }

    @Test
    fun `behandler hver lagret periode for seg`() {
        val urørt = lagret("Urørt", "2024-01-01", "2024-06-30")
        val forkortet = lagret("Forkortet", "2025-01-01", "2025-12-31")
        val dekket = lagret("Dekket", "2026-01-01", "2026-03-31")

        lagre(urørt, forkortet, dekket)(nyPeriode("2025-07-01", "2026-06-30"))

        verify(exactly = 0) { repository.deleteById(urørt.id) }
        verify(exactly = 1) { repository.deleteById(forkortet.id) }
        verify(exactly = 1) { repository.deleteById(dekket.id) }
        assertThat(biterAv("Urørt")).isEmpty()
        assertThat(biterAv("Dekket")).isEmpty()
        assertThat(biterAv("Forkortet")).hasSize(1)
        assertThat(biterAv("Forkortet")[0].tilOgMedDato).isEqualTo(dato("2025-06-30"))
    }

    @Test
    fun `beholder vilkårets egne felt og sporbar når perioden forkortes`() {
        val overlappet = lagret("Overlappet", "2025-01-01", "2025-06-30")

        lagre(overlappet)(nyPeriode("2025-03-01", "2025-12-31"))

        val forkortet = biterAv("Overlappet").single()
        assertThat(forkortet.behandlingId).isEqualTo(overlappet.behandlingId)
        assertThat(forkortet.regelverk).isEqualTo(Regelverk.EØS_FORORDNINGEN)
        assertThat(forkortet.vurdering).isEqualTo(Vurdering.NEI)
        assertThat(forkortet.sporbar).isEqualTo(overlappet.sporbar)
    }

    @Test
    fun `forkorter naboperioder også når en lagret periode endres`() {
        val endret = lagret("Endret", "2025-01-01", "2025-03-31")
        val nabo = lagret("Nabo", "2025-04-01", "2025-12-31")
        every { repository.findById(endret.id) } returns Optional.of(endret)

        lagre(endret, nabo)(nyPeriode("2025-01-01", "2025-06-30", id = endret.id))

        verify(exactly = 0) { repository.deleteById(endret.id) }
        verify(exactly = 1) { repository.deleteById(nabo.id) }
        assertThat(biterAv("Nabo")).hasSize(1)
        assertThat(biterAv("Nabo")[0].fraOgMedDato).isEqualTo(dato("2025-07-01"))
    }

    @Test
    fun `registrerer bare saksbehandlerens egen endring i endringshistorikken`() {
        lagre(lagret("Splittet", "2025-01-01", "2025-12-31"))(nyPeriode("2025-06-01", "2025-06-30"))

        verify(exactly = 1) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `skriver ingenting når til og med-dato er før fra og med-dato`() {
        val urørt = lagret("Urørt", "2025-01-01", "2025-06-30")

        assertThatThrownBy { lagre(urørt)(nyPeriode("2025-06-01", "2025-01-01")) }
            .isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)

        verify(exactly = 0) { repository.deleteById(any()) }
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `skriver ingenting når behandlingen ikke er redigerbar`() {
        val urørt = lagret("Urørt", "2025-01-01", "2025-06-30")
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar")

        assertThatThrownBy { lagre(urørt)(nyPeriode("2025-03-01", "2025-12-31")) }.isInstanceOf(Feil::class.java)

        verify(exactly = 0) { repository.deleteById(any()) }
        verify(exactly = 0) { repository.insert(any()) }
    }
}
