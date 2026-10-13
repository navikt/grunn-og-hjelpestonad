package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRepository
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRequest
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

private const val DIABETES_TYPE_1 = "E109"
private const val CØLIAKI = "K900"

class VilkårDiagnoseServiceTest {
    private val repository = mockk<VilkårDiagnoseRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service =
        VilkårDiagnoseService(
            repository,
            behandlingService,
            endringshistorikkService,
            ansvarligSaksbehandlerService,
        )

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.insertAll(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    private fun request(
        id: UUID? = null,
        kode: String = DIABETES_TYPE_1,
        erYrkesskade: Boolean = false,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårDiagnoseRequest(
        id = id,
        kode = kode,
        erYrkesskade = erYrkesskade,
        vurdering = Vurdering.JA,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun diagnose(
        kode: String = DIABETES_TYPE_1,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
        erYrkesskade: Boolean = false,
        vurdering: Vurdering = Vurdering.JA,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        kode = kode,
        tekst = "Tekst fra kodeverket",
        vurdering = vurdering,
        begrunnelse = "Eksisterende",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
        erYrkesskade = erYrkesskade,
    )

    @Test
    fun `lagrePeriode lagrer ikke når en regel brytes`() {
        assertThatThrownBy { service.lagrePeriode(behandlingId, request(kode = "X999")) }
            .isInstanceOf(UgyldigInput::class.java)
            .hasMessage("Diagnosekoden finnes ikke i ICD-10")

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `lagrePeriode normaliserer koden og henter teksten fra kodeverket`() {
        val resultat = service.lagrePeriode(behandlingId, request(kode = " e10.9 "))

        assertThat(resultat.kode).isEqualTo(DIABETES_TYPE_1)
        assertThat(resultat.tekst).isEqualTo("Diabetes mellitus type 1 uten komplikasjoner")
    }

    @Test
    fun `lagrePeriode lagrer yrkesskade med skadedato som fra og med-dato`() {
        val resultat = service.lagrePeriode(behandlingId, request(erYrkesskade = true, fraOgMedDato = LocalDate.of(2020, 3, 1)))

        assertThat(resultat.erYrkesskade).isTrue()
        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2020, 3, 1))
    }

    @Test
    fun `lagrePeriode tillater at ulike diagnoser løper samtidig`() {
        val eksisterende = diagnose(DIABETES_TYPE_1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns
            listOf(eksisterende)
        val innlagtePerioder = slot<List<VilkårDiagnose>>()
        every { repository.insertAll(capture(innlagtePerioder)) } answers { firstArg() }

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(kode = CØLIAKI, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )

        assertThat(resultat.kode).isEqualTo(CØLIAKI)
        assertThat(innlagtePerioder.captured).hasSize(2)
        assertThat(innlagtePerioder.captured.map { it.id }).contains(eksisterende.id)
        assertThat(innlagtePerioder.captured.map { it.kode }).containsExactlyInAnyOrder(DIABETES_TYPE_1, CØLIAKI)
        verify(exactly = 1) { repository.deleteByBehandlingId(behandlingId) }
    }

    @Test
    fun `lagrePeriode forkorter overlappende periode for samme diagnose`() {
        val eksisterende = diagnose(DIABETES_TYPE_1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)
        val innlagtePerioder = slot<List<VilkårDiagnose>>()
        every { repository.insertAll(capture(innlagtePerioder)) } answers { firstArg() }

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(kode = DIABETES_TYPE_1, fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
            )

        verify(exactly = 1) { repository.deleteByBehandlingId(behandlingId) }
        assertThat(innlagtePerioder.captured).hasSize(2)
        assertThat(innlagtePerioder.captured.single { it.id == resultat.id }).isEqualTo(resultat)
        assertThat(innlagtePerioder.captured.single { it.begrunnelse == eksisterende.begrunnelse }.tilOgMedDato)
            .isEqualTo(LocalDate.of(2025, 5, 31))
    }

    @Test
    fun `lagrePeriode kjenner igjen samme diagnose når koden skrives med punktum og små bokstaver`() {
        val eksisterende = diagnose(DIABETES_TYPE_1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)
        val innlagtePerioder = slot<List<VilkårDiagnose>>()
        every { repository.insertAll(capture(innlagtePerioder)) } answers { firstArg() }

        service.lagrePeriode(
            behandlingId,
            request(kode = " e10.9 ", fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
        )

        verify(exactly = 1) { repository.deleteByBehandlingId(behandlingId) }
        assertThat(innlagtePerioder.captured.single { it.begrunnelse == eksisterende.begrunnelse }.tilOgMedDato)
            .isEqualTo(LocalDate.of(2025, 5, 31))
    }

    @Test
    fun `lagrePeriode rører ikke perioder for en annen diagnose`() {
        val eksisterende = diagnose(CØLIAKI, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns
            listOf(eksisterende)
        val innlagtePerioder = slot<List<VilkårDiagnose>>()
        every { repository.insertAll(capture(innlagtePerioder)) } answers { firstArg() }

        service.lagrePeriode(
            behandlingId,
            request(kode = DIABETES_TYPE_1, fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
        )
        assertThat(innlagtePerioder.captured.single { it.id == eksisterende.id }).isEqualTo(eksisterende)
        assertThat(innlagtePerioder.captured.map { it.kode }).containsExactlyInAnyOrder(CØLIAKI, DIABETES_TYPE_1)
        verify(exactly = 1) { repository.deleteByBehandlingId(behandlingId) }
    }

    @Test
    fun `lagrePeriode tillater at samme diagnose periodiseres etter hverandre`() {
        every { repository.findByBehandlingId(any()) } returns
            listOf(diagnose(DIABETES_TYPE_1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)))

        service.lagrePeriode(
            behandlingId,
            request(kode = DIABETES_TYPE_1, fraOgMedDato = LocalDate.of(2025, 7, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
        )

        verify(exactly = 1) { repository.insertAll(any()) }
        verify(exactly = 1) { repository.deleteByBehandlingId(behandlingId) }
    }

    @Test
    fun `lagrePeriode avviser id som ikke tilhører behandlingen før det henter perioder eller skriver`() {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns Optional.empty()

        assertThatThrownBy { service.lagrePeriode(behandlingId, request(id = id)) }.isInstanceOf(Feil::class.java)

        verify(exactly = 0) { repository.findByBehandlingId(any()) }
        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `lagrePeriode avviser flere oppfylte yrkesskadeperioder før noe skrives`() {
        every { repository.findByBehandlingId(any()) } returns
            listOf(diagnose(DIABETES_TYPE_1, LocalDate.of(2020, 3, 1), LocalDate.of(2020, 12, 31), erYrkesskade = true))

        assertThatThrownBy {
            service.lagrePeriode(
                behandlingId,
                request(
                    kode = DIABETES_TYPE_1,
                    erYrkesskade = false,
                    fraOgMedDato = LocalDate.of(2020, 1, 1),
                    tilOgMedDato = LocalDate.of(2020, 6, 30),
                ),
            )
        }.isInstanceOf(UgyldigInput::class.java)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `registrerer endringen én gang når lagret periode splittes`() {
        val eksisterende = diagnose(DIABETES_TYPE_1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)

        service.lagrePeriode(
            behandlingId,
            request(kode = DIABETES_TYPE_1, fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
        )

        verify(exactly = 1) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `skriver ingenting når behandlingen ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar")

        assertThatThrownBy { service.lagrePeriode(behandlingId, request()) }.isInstanceOf(Feil::class.java)

        verify(exactly = 0) { repository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { repository.insertAll(any()) }
    }

    @Test
    fun `endringshistorikken røper ikke diagnosen`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(
            behandlingId,
            request(
                kode = DIABETES_TYPE_1,
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            ),
        )

        assertThat(detaljer.captured).isEqualTo("${VilkårType.DIAGNOSE}: ${Vurdering.JA}, Periode: 01.01.2025 – 30.06.2025")
        assertThat(detaljer.captured).doesNotContain(DIABETES_TYPE_1).doesNotContain("Diabetes")
    }
}
