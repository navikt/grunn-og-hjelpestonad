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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

class VilkårVurderingServiceTest {
    private val vilkårVurderingRepository = mockk<VilkårVurderingRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val vilkårVurderingService = VilkårVurderingService(vilkårVurderingRepository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    private val behandlingId = UUID.randomUUID()

    init {
        every { vilkårVurderingRepository.insert(any()) } answers { firstArg() }
        every { vilkårVurderingRepository.update(any()) } answers { firstArg() }
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns emptyList()
    }

    private fun request(
        id: UUID? = null,
        vilkårType: VilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE,
        vurdering: Vurdering = Vurdering.JA,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårVurderingRequest(
        id = id,
        vilkårType = vilkårType,
        vurdering = vurdering,
        begrunnelse = "Test",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    private fun vurdering(
        id: UUID = UUID.randomUUID(),
        behandlingId: UUID = this.behandlingId,
        vilkårType: VilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE,
        fraOgMedDato: LocalDate? = null,
        tilOgMedDato: LocalDate? = null,
    ) = VilkårVurdering(
        id = id,
        behandlingId = behandlingId,
        vilkårType = vilkårType,
        vurdering = Vurdering.JA,
        begrunnelse = "Eksisterende",
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

    @Test
    fun `lagreVilkårVurdering kaster feil når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { vilkårVurderingService.lagreVilkårVurdering(behandlingId, request()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `lagreVilkårVurdering kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { vilkårVurderingService.lagreVilkårVurdering(behandlingId, request()) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `lagreVilkårVurdering uten id oppretter ny periode`() {
        val resultat =
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
            )

        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultat.tilOgMedDato).isEqualTo(LocalDate.of(2025, 6, 30))
        verify(exactly = 1) { vilkårVurderingRepository.insert(any()) }
        verify(exactly = 0) { vilkårVurderingRepository.update(any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPRETTET, any()) }
    }

    @Test
    fun `lagreVilkårVurdering med id oppdaterer eksisterende periode`() {
        val eksisterende = vurdering(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { vilkårVurderingRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns listOf(eksisterende)

        val resultat =
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(id = eksisterende.id, vurdering = Vurdering.NEI, fraOgMedDato = LocalDate.of(2025, 2, 1)),
            )

        assertThat(resultat.id).isEqualTo(eksisterende.id)
        assertThat(resultat.vurdering).isEqualTo(Vurdering.NEI)
        assertThat(resultat.fraOgMedDato).isEqualTo(LocalDate.of(2025, 2, 1))
        verify(exactly = 1) { vilkårVurderingRepository.update(any()) }
        verify(exactly = 0) { vilkårVurderingRepository.insert(any()) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPDATERT, any()) }
    }

    @Test
    fun `lagreVilkårVurdering tillater flere perioder for samme vilkårstype`() {
        val eksisterende = vurdering(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns listOf(eksisterende)

        val resultat =
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 7, 1), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )

        assertThat(resultat.id).isNotEqualTo(eksisterende.id)
        assertThat(resultat.vilkårType).isEqualTo(eksisterende.vilkårType)
        verify(exactly = 1) { vilkårVurderingRepository.insert(any()) }
    }

    @Test
    fun `lagreVilkårVurdering avviser overlappende periode for samme vilkårstype`() {
        val eksisterende = vurdering(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns listOf(eksisterende)

        assertThatThrownBy {
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 6, 30), tilOgMedDato = LocalDate.of(2025, 12, 31)),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("overlapper")

        verify(exactly = 0) { vilkårVurderingRepository.insert(any()) }
    }

    @Test
    fun `lagreVilkårVurdering avviser to løpende perioder uten datoer for samme vilkårstype`() {
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns listOf(vurdering())

        assertThatThrownBy { vilkårVurderingService.lagreVilkårVurdering(behandlingId, request()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("overlapper")
    }

    @Test
    fun `lagreVilkårVurdering regner ikke perioden som overlappende med seg selv`() {
        val eksisterende = vurdering(fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30))
        every { vilkårVurderingRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)
        every { vilkårVurderingRepository.findByBehandlingIdAndVilkårType(any(), any()) } returns listOf(eksisterende)

        val resultat =
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(id = eksisterende.id, fraOgMedDato = LocalDate.of(2025, 1, 1), tilOgMedDato = LocalDate.of(2025, 6, 30)),
            )

        assertThat(resultat.id).isEqualTo(eksisterende.id)
    }

    @Test
    fun `lagreVilkårVurdering avviser perioder for ulike vilkårstyper som ikke overlapper hverandre`() {
        val annenType = vurdering(vilkårType = VilkårType.NØDVENDIGE_EKSTRAUTGIFTER)
        every {
            vilkårVurderingRepository.findByBehandlingIdAndVilkårType(behandlingId, VilkårType.NØDVENDIGE_EKSTRAUTGIFTER)
        } returns listOf(annenType)
        every {
            vilkårVurderingRepository.findByBehandlingIdAndVilkårType(behandlingId, VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE)
        } returns emptyList()

        vilkårVurderingService.lagreVilkårVurdering(behandlingId, request(vilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE))

        verify(exactly = 1) { vilkårVurderingRepository.insert(any()) }
    }

    @Test
    fun `lagreVilkårVurdering avviser til og med-dato før fra og med-dato`() {
        assertThatThrownBy {
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(fraOgMedDato = LocalDate.of(2025, 6, 1), tilOgMedDato = LocalDate.of(2025, 1, 1)),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("kan ikke være før")
    }

    @Test
    fun `lagreVilkårVurdering med id på en annen behandling gir ikke tilgang til perioden`() {
        val annenBehandling = vurdering(behandlingId = UUID.randomUUID())
        every { vilkårVurderingRepository.findById(annenBehandling.id) } returns Optional.of(annenBehandling)

        assertThatThrownBy { vilkårVurderingService.lagreVilkårVurdering(behandlingId, request(id = annenBehandling.id)) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Fant ikke vilkårsvurdering")

        verify(exactly = 0) { vilkårVurderingRepository.update(any()) }
    }

    @Test
    fun `lagreVilkårVurdering avviser endring av vilkårstype på en eksisterende periode`() {
        val eksisterende = vurdering(vilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE)
        every { vilkårVurderingRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)

        assertThatThrownBy {
            vilkårVurderingService.lagreVilkårVurdering(
                behandlingId,
                request(id = eksisterende.id, vilkårType = VilkårType.NØDVENDIGE_EKSTRAUTGIFTER),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Kan ikke endre vilkårstype")

        verify(exactly = 0) { vilkårVurderingRepository.update(any()) }
    }

    @Test
    fun `lagreVilkårVurdering skriver bare vilkårstype og vurdering til endringshistorikk`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        vilkårVurderingService.lagreVilkårVurdering(behandlingId, request())

        assertThat(detaljer.captured).isEqualTo("${VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE}: ${Vurdering.JA}")
    }

    @Test
    fun `slettVilkårVurdering sletter perioden og registrerer endringen`() {
        val eksisterende = vurdering()
        every { vilkårVurderingRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)

        vilkårVurderingService.slettVilkårVurdering(behandlingId, eksisterende.id)

        verify(exactly = 1) { vilkårVurderingRepository.deleteById(eksisterende.id) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_SLETTET, any()) }
    }

    @Test
    fun `slettVilkårVurdering sletter ikke periode som hører til en annen behandling`() {
        val annenBehandling = vurdering(behandlingId = UUID.randomUUID())
        every { vilkårVurderingRepository.findById(annenBehandling.id) } returns Optional.of(annenBehandling)

        assertThatThrownBy { vilkårVurderingService.slettVilkårVurdering(behandlingId, annenBehandling.id) }
            .isInstanceOf(Feil::class.java)
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.NOT_FOUND)

        verify(exactly = 0) { vilkårVurderingRepository.deleteById(any()) }
    }

    @Test
    fun `slettVilkårVurdering kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val eksisterende = vurdering()
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { vilkårVurderingService.slettVilkårVurdering(behandlingId, eksisterende.id) }
            .isInstanceOf(ManglerTilgang::class.java)

        verify(exactly = 0) { vilkårVurderingRepository.deleteById(any()) }
    }

    @Test
    fun `slettVilkårVurdering kaster feil når behandling ikke er redigerbar`() {
        val eksisterende = vurdering()
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { vilkårVurderingService.slettVilkårVurdering(behandlingId, eksisterende.id) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { vilkårVurderingRepository.deleteById(any()) }
    }
}
