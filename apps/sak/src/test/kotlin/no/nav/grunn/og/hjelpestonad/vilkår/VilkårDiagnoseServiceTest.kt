package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRepository
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRequest
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class VilkårDiagnoseServiceTest {
    private val repository = mockk<VilkårDiagnoseRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service = VilkårDiagnoseService(repository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.insert(any()) } answers { firstArg() }
        every { repository.update(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    private fun request(
        id: UUID? = null,
        diagnose: String = "Diabetes type 1",
        erYrkesskade: Boolean = false,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårDiagnoseRequest(
        id = id,
        diagnose = diagnose,
        erYrkesskade = erYrkesskade,
        vurdering = Vurdering.JA,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun diagnose(
        diagnose: String = "Diabetes type 1",
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårDiagnose(
        behandlingId = behandlingId,
        diagnose = diagnose,
        vurdering = Vurdering.JA,
        begrunnelse = "Eksisterende",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    @Test
    fun `lagrePeriode avviser tom diagnose`() {
        assertThatThrownBy { service.lagrePeriode(behandlingId, request(diagnose = "   ")) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Diagnose kan ikke være tom")
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `lagrePeriode trimmer diagnosen før lagring`() {
        val resultat = service.lagrePeriode(behandlingId, request(diagnose = "  Diabetes type 1  "))

        assertThat(resultat.diagnose).isEqualTo("Diabetes type 1")
    }

    @Test
    fun `lagrePeriode lagrer yrkesskade med skadedato som fra og med-dato`() {
        val resultat = service.lagrePeriode(behandlingId, request(erYrkesskade = true, fraOgMedDato = LocalDate.of(2020, 3, 1)))

        assertThat(resultat.erYrkesskade).isTrue()
        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2020, 3, 1))
    }

    @Test
    fun `lagrePeriode tillater at ulike diagnoser løper samtidig`() {
        every { repository.findByBehandlingId(any()) } returns
            listOf(diagnose("Diabetes type 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(diagnose = "Cøliaki", fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )

        assertThat(resultat.diagnose).isEqualTo("Cøliaki")
        verify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `lagrePeriode forkorter overlappende periode for samme diagnose`() {
        val eksisterende = diagnose("Diabetes type 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)
        val innsatte = mutableListOf<VilkårDiagnose>()
        every { repository.insert(capture(innsatte)) } answers { firstArg() }

        service.lagrePeriode(
            behandlingId,
            request(diagnose = "Diabetes type 1", fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
        )

        verify(exactly = 1) { repository.deleteById(eksisterende.id) }
        assertThat(innsatte.single { it.begrunnelse == eksisterende.begrunnelse }.tilOgMedDato)
            .isEqualTo(LocalDate.of(2025, 5, 31))
    }

    @Test
    fun `lagrePeriode kjenner igjen samme diagnose uavhengig av store og små bokstaver`() {
        val eksisterende = diagnose("Diabetes type 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)
        val innsatte = mutableListOf<VilkårDiagnose>()
        every { repository.insert(capture(innsatte)) } answers { firstArg() }

        service.lagrePeriode(
            behandlingId,
            request(diagnose = " diabetes TYPE 1 ", fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
        )

        verify(exactly = 1) { repository.deleteById(eksisterende.id) }
        assertThat(innsatte.single { it.begrunnelse == eksisterende.begrunnelse }.tilOgMedDato)
            .isEqualTo(LocalDate.of(2025, 5, 31))
    }

    @Test
    fun `lagrePeriode rører ikke perioder for en annen diagnose`() {
        every { repository.findByBehandlingId(any()) } returns
            listOf(diagnose("Cøliaki", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))

        service.lagrePeriode(
            behandlingId,
            request(diagnose = "Diabetes type 1", fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2026, 1, 31)),
        )

        verify(exactly = 0) { repository.deleteById(any()) }
        verify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `lagrePeriode tillater at samme diagnose periodiseres etter hverandre`() {
        every { repository.findByBehandlingId(any()) } returns
            listOf(diagnose("Diabetes type 1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)))

        service.lagrePeriode(
            behandlingId,
            request(diagnose = "Diabetes type 1", fraOgMedDato = LocalDate.of(2025, 7, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
        )

        verify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `endringshistorikken røper ikke diagnosen`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(behandlingId, request(diagnose = "Diabetes type 1"))

        assertThat(detaljer.captured).isEqualTo("${VilkårType.DIAGNOSE}: ${Vurdering.JA}")
        assertThat(detaljer.captured).doesNotContain("Diabetes")
    }
}
