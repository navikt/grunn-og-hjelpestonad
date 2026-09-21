package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRepository
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRequest
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

/**
 * Dekker livssyklusen som ligger i [VilkårPeriodeService] og deles av alle tre vilkårene.
 * Medlemskap er valgt som representant fordi det ikke har vilkårsspesifikke særregler.
 */
class VilkårMedlemskapServiceTest {
    private val repository = mockk<VilkårMedlemskapRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service = VilkårMedlemskapService(repository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    private val behandlingId = UUID.randomUUID()

    init {
        every { repository.insert(any()) } answers { firstArg() }
        every { repository.update(any()) } answers { firstArg() }
        every { repository.findByBehandlingId(any()) } returns emptyList()
    }

    private fun request(
        id: UUID? = null,
        regelverk: Regelverk = Regelverk.NASJONALE_REGLER,
        vurdering: Vurdering = Vurdering.JA,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårMedlemskapRequest(
        id = id,
        regelverk = regelverk,
        vurdering = vurdering,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun periode(
        id: UUID = UUID.randomUUID(),
        behandlingId: UUID = this.behandlingId,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårMedlemskap(
        id = id,
        behandlingId = behandlingId,
        regelverk = Regelverk.NASJONALE_REGLER,
        vurdering = Vurdering.JA,
        begrunnelse = "Eksisterende",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    @Test
    fun `lagrePeriode kaster feil når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { service.lagrePeriode(behandlingId, request()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `lagrePeriode kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { service.lagrePeriode(behandlingId, request()) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `lagrePeriode uten id oppretter ny periode`() {
        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
            )

        assertThat(resultat.behandlingId).isEqualTo(behandlingId)
        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultat.tilOgMedDato).isEqualTo(LocalDate.of(2025, 6, 30))
        verify(exactly = 1) { repository.insert(any()) }
        verify(exactly = 0) { repository.update(any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPRETTET, any()) }
    }

    @Test
    fun `lagrePeriode med id oppdaterer eksisterende periode`() {
        val eksisterende = periode(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { repository.findById(eksisterende.id) } returns Optional.of(eksisterende)
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(
                    id = eksisterende.id,
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                    vurdering = Vurdering.NEI,
                    fraOgMedDato = LocalDate.of(2025, 2, 1),
                ),
            )

        assertThat(resultat.id).isEqualTo(eksisterende.id)
        assertThat(resultat.regelverk).isEqualTo(Regelverk.EØS_FORORDNINGEN)
        assertThat(resultat.vurdering).isEqualTo(Vurdering.NEI)
        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2025, 2, 1))
        verify(exactly = 1) { repository.update(any()) }
        verify(exactly = 0) { repository.insert(any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPDATERT, any()) }
    }

    @Test
    fun `lagrePeriode tillater flere perioder for samme vilkår`() {
        val eksisterende = periode(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 7, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )

        assertThat(resultat.id).isNotEqualTo(eksisterende.id)
        verify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `lagrePeriode regner ikke perioden som overlappende med seg selv`() {
        val eksisterende = periode(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { repository.findById(eksisterende.id) } returns Optional.of(eksisterende)
        every { repository.findByBehandlingId(any()) } returns listOf(eksisterende)

        val resultat =
            service.lagrePeriode(
                behandlingId,
                request(id = eksisterende.id, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
            )

        assertThat(resultat.id).isEqualTo(eksisterende.id)
    }

    @Test
    fun `lagrePeriode avviser til og med-dato før fra og med-dato`() {
        assertThatThrownBy {
            service.lagrePeriode(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2025, 1, 1)),
            )
        }.isInstanceOfAny(IllegalArgumentException::class.java, IllegalStateException::class.java)
    }

    @Test
    fun `lagrePeriode med id på en annen behandling gir ikke tilgang til perioden`() {
        val annenBehandling = periode(behandlingId = UUID.randomUUID())
        every { repository.findById(annenBehandling.id) } returns Optional.of(annenBehandling)

        assertThatThrownBy { service.lagrePeriode(behandlingId, request(id = annenBehandling.id)) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Fant ikke vilkårsvurdering")

        verify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `lagrePeriode skriver vilkårstype, vurdering og periode til endringshistorikk`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.lagrePeriode(
            behandlingId,
            request(
                fraOgMedDato = LocalDate.of(2025, 1, 1),
                tilOgMedDato = LocalDate.of(2025, 6, 30),
            ),
        )

        assertThat(detaljer.captured).isEqualTo(
            "${VilkårType.MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN}: ${Vurdering.JA}, Periode: 01.01.2025 – 30.06.2025",
        )
    }

    @Test
    fun `slettPeriode sletter perioden og registrerer endringen`() {
        val eksisterende = periode()
        every { repository.findById(eksisterende.id) } returns Optional.of(eksisterende)

        service.slettPeriode(behandlingId, eksisterende.id)

        verify(exactly = 1) { repository.deleteById(eksisterende.id) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_SLETTET, any()) }
    }

    @Test
    fun `slettPeriode sletter ikke periode som hører til en annen behandling`() {
        val annenBehandling = periode(behandlingId = UUID.randomUUID())
        every { repository.findById(annenBehandling.id) } returns Optional.of(annenBehandling)

        assertThatThrownBy { service.slettPeriode(behandlingId, annenBehandling.id) }
            .isInstanceOf(Feil::class.java)
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.NOT_FOUND)

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `slettPeriode kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { service.slettPeriode(behandlingId, UUID.randomUUID()) }
            .isInstanceOf(ManglerTilgang::class.java)

        verify(exactly = 0) { repository.deleteById(any()) }
    }

    @Test
    fun `slettPeriode kaster feil når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { service.slettPeriode(behandlingId, UUID.randomUUID()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { repository.deleteById(any()) }
    }
}
