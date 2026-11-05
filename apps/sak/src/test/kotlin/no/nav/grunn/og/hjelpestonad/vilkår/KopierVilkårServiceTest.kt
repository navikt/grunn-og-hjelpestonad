package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseRepository
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapRepository
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KopierVilkårServiceTest {
    private val medlemskapRepository = mockk<VilkårMedlemskapRepository>()
    private val diagnoseRepository = mockk<VilkårDiagnoseRepository>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)

    private val kopierVilkårService =
        KopierVilkårService(
            vilkårMedlemskapService =
                VilkårMedlemskapService(medlemskapRepository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService),
            vilkårDiagnoseService =
                VilkårDiagnoseService(diagnoseRepository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService),
        )

    private val forrigeBehandlingId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val behandlingId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val medlemskapInsert = slot<List<VilkårMedlemskap>>()
    private val diagnoseInsert = slot<List<VilkårDiagnose>>()

    init {
        every { medlemskapRepository.insertAll(capture(medlemskapInsert)) } answers { firstArg() }
        every { diagnoseRepository.insertAll(capture(diagnoseInsert)) } answers { firstArg() }
    }

    @Test
    fun `medlemskap vurdert til NEI kopieres ikke`() {
        forrigeBehandlingHar(medlemskap = listOf(medlemskap(vurdering = Vurdering.NEI)))

        kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)

        assertThat(medlemskapInsert.captured).isEmpty()
    }

    @Test
    fun `medlemskap vurdert til JA kopieres til den nye behandlingen`() {
        forrigeBehandlingHar(medlemskap = listOf(medlemskap(vurdering = Vurdering.JA)))

        kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)

        val kopi = medlemskapInsert.captured.single()
        assertThat(kopi.behandlingId).isEqualTo(behandlingId)
        assertThat(kopi.vurdering).isEqualTo(Vurdering.JA)
        assertThat(kopi.id).isNotEqualTo(MEDLEMSKAP_ID)
    }

    @Test
    fun `bare diagnoser vurdert til JA kopieres når forrige behandling har både JA og NEI`() {
        forrigeBehandlingHar(
            diagnose =
                listOf(
                    diagnose(id = DIAGNOSE_JA_ID, kode = "G35", tekst = "Multippel sklerose", vurdering = Vurdering.JA),
                    diagnose(id = DIAGNOSE_NEI_ID, kode = "J45", tekst = "Astma", vurdering = Vurdering.NEI),
                ),
        )

        kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)

        assertThat(diagnoseInsert.captured.map { it.kode }).containsExactly("G35")
        assertThat(diagnoseInsert.captured.single().behandlingId).isEqualTo(behandlingId)
    }

    @Test
    fun `ingen vilkår i forrige behandling gir ingen feil og ingen kopi`() {
        forrigeBehandlingHar()

        assertThatCode {
            kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)
        }.doesNotThrowAnyException()

        assertThat(medlemskapInsert.captured).isEmpty()
        assertThat(diagnoseInsert.captured).isEmpty()
    }

    @Test
    fun `kopi av vilkår får ny id og behandling og beholder vurderingen`() {
        val original =
            VilkårDiagnose(
                behandlingId = forrigeBehandlingId,
                vurdering = Vurdering.JA,
                begrunnelse = "Begrunnelse",
                fraOgMedDato = LocalDate.of(2026, 1, 1),
                tilOgMedDato = LocalDate.of(2026, 12, 31),
                sporbar = Sporbar(opprettetAv = "Z999999"),
                kode = "G35",
                tekst = "Multippel sklerose",
                erYrkesskade = true,
            )

        val kopi = original.kopierTilBehandling(behandlingId)

        assertNotEquals(original.id, kopi.id)
        assertEquals(behandlingId, kopi.behandlingId)
        assertNotEquals(original.sporbar.opprettetAv, kopi.sporbar.opprettetAv)
        assertEquals(original.copy(id = kopi.id, behandlingId = behandlingId, sporbar = kopi.sporbar), kopi)
    }

    private fun forrigeBehandlingHar(
        medlemskap: List<VilkårMedlemskap> = emptyList(),
        diagnose: List<VilkårDiagnose> = emptyList(),
    ) {
        every { medlemskapRepository.findByBehandlingId(forrigeBehandlingId) } returns medlemskap
        every { diagnoseRepository.findByBehandlingId(forrigeBehandlingId) } returns diagnose
    }

    private fun medlemskap(vurdering: Vurdering) =
        VilkårMedlemskap(
            id = MEDLEMSKAP_ID,
            behandlingId = forrigeBehandlingId,
            vurdering = vurdering,
            regelverk = Regelverk.NASJONALE_REGLER,
        )

    private fun diagnose(
        id: UUID,
        kode: String,
        tekst: String,
        vurdering: Vurdering,
    ) = VilkårDiagnose(
        id = id,
        behandlingId = forrigeBehandlingId,
        vurdering = vurdering,
        kode = kode,
        tekst = tekst,
    )

    private companion object {
        val MEDLEMSKAP_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val DIAGNOSE_JA_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000021")
        val DIAGNOSE_NEI_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")
    }
}
