package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårVurderingStegService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRett
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRettRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class VilkårVurderingStegServiceTest {
    private val vilkårMedlemskapService = mockk<VilkårMedlemskapService>(relaxed = true)
    private val vilkårDiagnoseService = mockk<VilkårDiagnoseService>(relaxed = true)
    private val vilkårInstitusjonService = mockk<VilkårInstitusjonService>(relaxed = true)
    private val periodeMedRettRepository = mockk<PeriodeMedRettRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)

    private val service =
        VilkårVurderingStegService(
            vilkårMedlemskapService,
            vilkårDiagnoseService,
            vilkårInstitusjonService,
            periodeMedRettRepository,
            behandlingService,
            ansvarligSaksbehandlerService,
            endringshistorikkService,
        )

    private val behandlingId = UUID.randomUUID()

    init {
        every { periodeMedRettRepository.insertAll(any()) } answers { firstArg() }
        medKomplettGrunnlag()
    }

    @Test
    fun `fullførSteg kaster feil når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws
            Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { periodeMedRettRepository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { periodeMedRettRepository.insertAll(any()) }
    }

    @Test
    fun `fullførSteg kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(ManglerTilgang::class.java)

        verify(exactly = 0) { periodeMedRettRepository.deleteByBehandlingId(any()) }
    }

    @Test
    fun `fullførSteg validerer, sletter, setter inn og registrerer endring i riktig rekkefølge`() {
        service.fullførSteg(behandlingId)

        verifyOrder {
            behandlingService.validerBehandlingErRedigerbar(behandlingId)
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            vilkårMedlemskapService.hentPerioder(behandlingId)
            vilkårDiagnoseService.hentPerioder(behandlingId)
            vilkårInstitusjonService.hentPerioder(behandlingId)
            periodeMedRettRepository.deleteByBehandlingId(behandlingId)
            periodeMedRettRepository.insertAll(any())
            endringshistorikkService.registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_FULLFØRT, any())
        }
    }

    @Test
    fun `fullførSteg kaster VilkårValideringFeil og skriver ingenting når et vilkår mangler`() {
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns emptyList()

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(VilkårValideringFeil::class.java)
            .extracting { (it as VilkårValideringFeil).feil.map { feil -> feil.vilkårType } }
            .isEqualTo(listOf(VilkårType.DIAGNOSE))

        verify(exactly = 0) { periodeMedRettRepository.deleteByBehandlingId(any()) }
        verify(exactly = 0) { periodeMedRettRepository.insertAll(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg lagrer de utledede periodene`() {
        val lagrede = slot<List<PeriodeMedRett>>()
        every { periodeMedRettRepository.insertAll(capture(lagrede)) } answers { firstArg() }

        val resultat = service.fullførSteg(behandlingId)

        assertThat(lagrede.captured).isEqualTo(resultat)
        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().behandlingId).isEqualTo(behandlingId)
        assertThat(resultat.single().fraOgMedDato).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(resultat.single().tilOgMedDato).isEqualTo(LocalDate.of(2025, 12, 31))
    }

    @Test
    fun `fullførSteg skriver tom liste når ingen perioder oppfyller alle vilkår`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)

        val resultat = service.fullførSteg(behandlingId)

        assertThat(resultat).isEmpty()
        verify(exactly = 1) { periodeMedRettRepository.deleteByBehandlingId(behandlingId) }
        verify(exactly = 1) { periodeMedRettRepository.insertAll(emptyList()) }
    }

    @Test
    fun `fullførSteg er idempotent - andre kall gir samme perioder`() {
        val første = service.fullførSteg(behandlingId).map { it.fraOgMedDato to it.tilOgMedDato }
        val andre = service.fullførSteg(behandlingId).map { it.fraOgMedDato to it.tilOgMedDato }

        assertThat(andre).isEqualTo(første)
        verify(exactly = 2) { periodeMedRettRepository.deleteByBehandlingId(behandlingId) }
    }

    @Test
    fun `endringshistorikken inneholder antall perioder og datoer, men ingen diagnoseopplysninger`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.fullførSteg(behandlingId)

        assertThat(detaljer.captured).isEqualTo("1 perioder med oppfylte vilkår: 01.01.2025 – 31.12.2025")
        assertThat(detaljer.captured).doesNotContain("Diabetes")
    }

    @Test
    fun `endringshistorikken sier ifra når ingen perioder har oppfylte vilkår`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.fullførSteg(behandlingId)

        assertThat(detaljer.captured).isEqualTo("Ingen perioder med oppfylte vilkår")
    }

    private fun medKomplettGrunnlag(medlemskapVurdering: Vurdering = Vurdering.JA) {
        every { vilkårMedlemskapService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = medlemskapVurdering,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 12, 31),
                ),
            )
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårDiagnose(
                    behandlingId = behandlingId,
                    diagnose = "Diabetes type 1",
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 12, 31),
                ),
            )
        every { vilkårInstitusjonService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårInstitusjon(
                    behandlingId = behandlingId,
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 12, 31),
                ),
            )
    }
}
