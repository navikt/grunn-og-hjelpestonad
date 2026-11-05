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
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårStegService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.NesteStegEtterInngangsvilkår
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PeriodeMedOppfylteInngangsvilkår
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class InngangsvilkårStegServiceTest {
    private val vilkårMedlemskapService = mockk<VilkårMedlemskapService>(relaxed = true)
    private val vilkårDiagnoseService = mockk<VilkårDiagnoseService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)

    private val service =
        InngangsvilkårStegService(
            vilkårMedlemskapService,
            vilkårDiagnoseService,
            behandlingService,
            ansvarligSaksbehandlerService,
            endringshistorikkService,
        )

    private val behandlingId = UUID.randomUUID()

    init {
        medKomplettGrunnlag()
    }

    @Test
    fun `fullførSteg kaster feil og skriver ingenting når behandling ikke er redigerbar`() {
        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws
            Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { vilkårMedlemskapService.hentPerioder(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg kaster feil og skriver ingenting når bruker ikke er ansvarlig saksbehandler`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)

        verify(exactly = 0) { vilkårMedlemskapService.hentPerioder(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg sjekker tilgang før den henter vilkårene og registrerer endringen til slutt`() {
        service.fullførSteg(behandlingId)

        verifyOrder {
            behandlingService.validerBehandlingErRedigerbar(behandlingId)
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            vilkårMedlemskapService.hentPerioder(behandlingId)
            vilkårDiagnoseService.hentPerioder(behandlingId)
            endringshistorikkService.registrerEndring(behandlingId, EndringType.INNGANGSVILKÅR_VURDERING_FULLFØRT, any())
        }
    }

    @Test
    fun `fullførSteg kaster VilkårValideringFeil og skriver ingenting når et inngangsvilkår mangler`() {
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns emptyList()

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(VilkårValideringFeil::class.java)
            .extracting { (it as VilkårValideringFeil).feil.map { feil -> feil.vilkårType } }
            .isEqualTo(listOf(VilkårType.DIAGNOSE))

        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg returnerer periodene og går videre til sats og trygdetid`() {
        val resultat = service.fullførSteg(behandlingId)

        assertThat(resultat.perioder).containsExactly(
            PeriodeMedOppfylteInngangsvilkår(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
        )
        assertThat(resultat.nesteSteg).isEqualTo(NesteStegEtterInngangsvilkår.SATS_OG_TRYGDETID)
    }

    @Test
    fun `fullførSteg hopper til simulering når det ikke er rett i noen måned`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)

        val resultat = service.fullførSteg(behandlingId)

        assertThat(resultat.perioder).isEmpty()
        assertThat(resultat.nesteSteg).isEqualTo(NesteStegEtterInngangsvilkår.SIMULERING)
    }

    @Test
    fun `endringshistorikken inneholder antall perioder og datoer, men ingen diagnoseopplysninger`() {
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.fullførSteg(behandlingId)

        assertThat(detaljer.captured).isEqualTo("1 periode med oppfylte inngangsvilkår: 01.01.2025 – 31.12.2025")
        assertThat(detaljer.captured).doesNotContain("Diabetes").doesNotContain("E109")
    }

    @Test
    fun `endringshistorikken bøyer perioder i flertall`() {
        every { vilkårMedlemskapService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 3, 31),
                ),
                VilkårMedlemskap(
                    behandlingId = behandlingId,
                    regelverk = Regelverk.NASJONALE_REGLER,
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 6, 1),
                    tilOgMedDato = LocalDate.of(2025, 12, 31),
                ),
            )
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.fullførSteg(behandlingId)

        assertThat(detaljer.captured)
            .isEqualTo("2 perioder med oppfylte inngangsvilkår: 01.01.2025 – 31.03.2025, 01.06.2025 – 31.12.2025")
    }

    @Test
    fun `endringshistorikken sier ifra når ingen perioder har oppfylte inngangsvilkår`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)
        val detaljer = slot<String>()
        every { endringshistorikkService.registrerEndring(any(), any(), capture(detaljer)) } returns Unit

        service.fullførSteg(behandlingId)

        assertThat(detaljer.captured).isEqualTo("Ingen perioder med oppfylte inngangsvilkår")
    }

    @Test
    fun `hentPerioder regner ut periodene uten å sjekke redigerbarhet eller skrive endringshistorikk`() {
        val perioder = service.hentPerioder(behandlingId)

        assertThat(perioder).containsExactly(
            PeriodeMedOppfylteInngangsvilkår(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
        )
        verify(exactly = 0) { behandlingService.validerBehandlingErRedigerbar(any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `hentPerioder validerer ikke, så steg 2 kan vises før alle inngangsvilkår er vurdert`() {
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns emptyList()

        assertThat(service.hentPerioder(behandlingId)).isEmpty()
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
                    kode = "E109",
                    tekst = "Diabetes mellitus type 1 uten komplikasjoner",
                    vurdering = Vurdering.JA,
                    fraOgMedDato = LocalDate.of(2025, 1, 1),
                    tilOgMedDato = LocalDate.of(2025, 12, 31),
                ),
            )
    }
}
