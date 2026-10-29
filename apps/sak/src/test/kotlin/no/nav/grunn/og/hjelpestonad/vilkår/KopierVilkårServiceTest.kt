package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KopierVilkårServiceTest {
    private val vilkårMedlemskapService = mockk<VilkårMedlemskapService>()
    private val vilkårDiagnoseService = mockk<VilkårDiagnoseService>()
    private val vilkårInstitusjonService = mockk<VilkårInstitusjonService>()
    private val kopierVilkårService =
        KopierVilkårService(
            vilkårMedlemskapService = vilkårMedlemskapService,
            vilkårDiagnoseService = vilkårDiagnoseService,
            vilkårInstitusjonService = vilkårInstitusjonService,
        )

    private val forrigeBehandlingId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()

    @Test
    fun `kopierer vilkårene for medlemskap, diagnose og institusjon`() {
        val medlemskap = medlemskap(behandlingId)
        every { vilkårMedlemskapService.kopierPerioder(forrigeBehandlingId, behandlingId) } returns listOf(medlemskap)
        every { vilkårDiagnoseService.kopierPerioder(forrigeBehandlingId, behandlingId) } returns emptyList()
        every { vilkårInstitusjonService.kopierPerioder(forrigeBehandlingId, behandlingId) } returns emptyList()

        kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)

        verify(exactly = 1) { vilkårMedlemskapService.kopierPerioder(forrigeBehandlingId, behandlingId) }
        verify(exactly = 1) { vilkårDiagnoseService.kopierPerioder(forrigeBehandlingId, behandlingId) }
        verify(exactly = 1) { vilkårInstitusjonService.kopierPerioder(forrigeBehandlingId, behandlingId) }
    }

    @Test
    fun `kaster feil når forrige behandling ikke har vilkår`() {
        every { vilkårMedlemskapService.kopierPerioder(any(), any()) } returns emptyList()
        every { vilkårDiagnoseService.kopierPerioder(any(), any()) } returns emptyList()
        every { vilkårInstitusjonService.kopierPerioder(any(), any()) } returns emptyList()

        assertThatThrownBy {
            kopierVilkårService.kopierFraForrigeBehandling(forrigeBehandlingId = forrigeBehandlingId, behandlingId = behandlingId)
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("ingen vilkår")
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

    private fun medlemskap(behandlingId: UUID) =
        VilkårMedlemskap(
            behandlingId = behandlingId,
            vurdering = Vurdering.JA,
            regelverk = Regelverk.NASJONALE_REGLER,
        )
}
