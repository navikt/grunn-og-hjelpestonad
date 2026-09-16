package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
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

class VilkårDiagnoseServiceTest {
    private val vilkårDiagnoseRepository = mockk<VilkårDiagnoseRepository>(relaxed = true)
    private val vilkårVurderingService = mockk<VilkårVurderingService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val vilkårDiagnoseService =
        VilkårDiagnoseService(
            vilkårDiagnoseRepository,
            vilkårVurderingService,
            behandlingService,
            endringshistorikkService,
            ansvarligSaksbehandlerService,
        )

    private val behandlingId = UUID.randomUUID()
    private val vurdering =
        VilkårVurdering(
            behandlingId = behandlingId,
            vilkårType = VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE,
            vurdering = Vurdering.JA,
            begrunnelse = "Varig sykdom",
        )

    init {
        every { vilkårDiagnoseRepository.insert(any()) } answers { firstArg() }
        every { vilkårDiagnoseRepository.update(any()) } answers { firstArg() }
        every { vilkårVurderingService.hentVurderingPåBehandling(behandlingId, vurdering.id) } returns vurdering
    }

    private fun diagnose(
        id: UUID = UUID.randomUUID(),
        vilkårVurderingId: UUID = vurdering.id,
        diagnose: String = "Diabetes type 1",
    ) = VilkårDiagnose(id = id, vilkårVurderingId = vilkårVurderingId, diagnose = diagnose)

    @Test
    fun `lagreDiagnose oppretter ny diagnose på vilkårsperioden`() {
        val resultat =
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId,
                vurdering.id,
                DiagnoseRequest(diagnose = "Diabetes type 1"),
            )

        assertThat(resultat.diagnose).isEqualTo("Diabetes type 1")
        assertThat(resultat.vilkårVurderingId).isEqualTo(vurdering.id)
        verify(exactly = 1) { vilkårDiagnoseRepository.insert(any()) }
    }

    @Test
    fun `lagreDiagnose støtter flere diagnoser på samme vilkårsperiode`() {
        vilkårDiagnoseService.lagreDiagnose(behandlingId, vurdering.id, DiagnoseRequest(diagnose = "Diabetes type 1"))
        vilkårDiagnoseService.lagreDiagnose(behandlingId, vurdering.id, DiagnoseRequest(diagnose = "Cøliaki"))

        verify(exactly = 2) { vilkårDiagnoseRepository.insert(any()) }
    }

    @Test
    fun `lagreDiagnose lagrer yrkesskade med dato`() {
        val resultat =
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId,
                vurdering.id,
                DiagnoseRequest(diagnose = "Støyskade", yrkesskade = true, yrkesskadeDato = LocalDate.of(2020, 3, 1)),
            )

        assertThat(resultat.yrkesskade).isTrue()
        assertThat(resultat.yrkesskadeDato).isEqualTo(LocalDate.of(2020, 3, 1))
    }

    @Test
    fun `lagreDiagnose med id oppdaterer eksisterende diagnose`() {
        val eksisterende = diagnose()
        every { vilkårDiagnoseRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)

        val resultat =
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId,
                vurdering.id,
                DiagnoseRequest(id = eksisterende.id, diagnose = "Cøliaki"),
            )

        assertThat(resultat.id).isEqualTo(eksisterende.id)
        assertThat(resultat.diagnose).isEqualTo("Cøliaki")
        verify(exactly = 1) { vilkårDiagnoseRepository.update(any()) }
        verify(exactly = 0) { vilkårDiagnoseRepository.insert(any()) }
    }

    @Test
    fun `lagreDiagnose avviser tom diagnose`() {
        assertThatThrownBy {
            vilkårDiagnoseService.lagreDiagnose(behandlingId, vurdering.id, DiagnoseRequest(diagnose = "   "))
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Diagnose kan ikke være tom")
    }

    @Test
    fun `lagreDiagnose avviser yrkesskadedato uten yrkesskadeflagg`() {
        assertThatThrownBy {
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId,
                vurdering.id,
                DiagnoseRequest(diagnose = "Støyskade", yrkesskade = false, yrkesskadeDato = LocalDate.of(2020, 3, 1)),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Yrkesskadedato")
    }

    @Test
    fun `lagreDiagnose gir ikke tilgang til diagnose på en annen vilkårsperiode`() {
        val annenPeriode = diagnose(vilkårVurderingId = UUID.randomUUID())
        every { vilkårDiagnoseRepository.findById(annenPeriode.id) } returns Optional.of(annenPeriode)

        assertThatThrownBy {
            vilkårDiagnoseService.lagreDiagnose(
                behandlingId,
                vurdering.id,
                DiagnoseRequest(id = annenPeriode.id, diagnose = "Cøliaki"),
            )
        }.isInstanceOf(Feil::class.java)
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.NOT_FOUND)

        verify(exactly = 0) { vilkårDiagnoseRepository.update(any()) }
    }

    @Test
    fun `lagreDiagnose kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy {
            vilkårDiagnoseService.lagreDiagnose(behandlingId, vurdering.id, DiagnoseRequest(diagnose = "Diabetes type 1"))
        }.isInstanceOf(ManglerTilgang::class.java)

        verify(exactly = 0) { vilkårDiagnoseRepository.insert(any()) }
    }

    @Test
    fun `lagreDiagnose kaster feil når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy {
            vilkårDiagnoseService.lagreDiagnose(behandlingId, vurdering.id, DiagnoseRequest(diagnose = "Diabetes type 1"))
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { vilkårDiagnoseRepository.insert(any()) }
    }

    @Test
    fun `lagreDiagnose skriver aldri diagnose til endringshistorikk`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        vilkårDiagnoseService.lagreDiagnose(
            behandlingId,
            vurdering.id,
            DiagnoseRequest(diagnose = "Diabetes type 1", yrkesskade = true, yrkesskadeDato = LocalDate.of(2020, 3, 1)),
        )

        assertThat(detaljer.captured).doesNotContain("Diabetes")
        assertThat(detaljer.captured).isEqualTo("${VilkårType.VARIG_SYKDOM_SKADE_ELLER_LYTE}: ${Vurdering.JA}")
    }

    @Test
    fun `slettDiagnose sletter diagnosen`() {
        val eksisterende = diagnose()
        every { vilkårDiagnoseRepository.findById(eksisterende.id) } returns Optional.of(eksisterende)

        vilkårDiagnoseService.slettDiagnose(behandlingId, vurdering.id, eksisterende.id)

        verify(exactly = 1) { vilkårDiagnoseRepository.deleteById(eksisterende.id) }
    }

    @Test
    fun `slettDiagnose sletter ikke diagnose på en annen vilkårsperiode`() {
        val annenPeriode = diagnose(vilkårVurderingId = UUID.randomUUID())
        every { vilkårDiagnoseRepository.findById(annenPeriode.id) } returns Optional.of(annenPeriode)

        assertThatThrownBy { vilkårDiagnoseService.slettDiagnose(behandlingId, vurdering.id, annenPeriode.id) }
            .isInstanceOf(Feil::class.java)
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.NOT_FOUND)

        verify(exactly = 0) { vilkårDiagnoseRepository.deleteById(any()) }
    }

    @Test
    fun `hentDiagnoser validerer at vilkårsperioden hører til behandlingen`() {
        val annenBehandling = UUID.randomUUID()
        every { vilkårVurderingService.hentVurderingPåBehandling(annenBehandling, vurdering.id) } throws
            Feil(melding = "Fant ikke vilkårsvurdering", httpStatus = HttpStatus.NOT_FOUND)

        assertThatThrownBy { vilkårDiagnoseService.hentDiagnoser(annenBehandling, vurdering.id) }
            .isInstanceOf(Feil::class.java)

        verify(exactly = 0) { vilkårDiagnoseRepository.findByVilkårVurderingId(any()) }
    }

    @Test
    fun `hentDiagnoser returnerer diagnosene på vilkårsperioden`() {
        every { vilkårDiagnoseRepository.findByVilkårVurderingId(vurdering.id) } returns
            listOf(diagnose(diagnose = "Diabetes type 1"), diagnose(diagnose = "Cøliaki"))

        val resultat = vilkårDiagnoseService.hentDiagnoser(behandlingId, vurdering.id)

        assertThat(resultat).hasSize(2)
        assertThat(resultat.map { it.diagnose }).containsExactlyInAnyOrder("Diabetes type 1", "Cøliaki")
    }
}
