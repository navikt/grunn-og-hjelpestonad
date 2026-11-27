package no.nav.grunn.og.hjelpestonad.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.behandling.utledStegEtterInngangsvilkår
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlag
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFaktagrunnlagService
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlFalskIdentitet
import no.nav.grunn.og.hjelpestonad.faktagrunnlag.pdl.PdlMetadata
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårBlokkertFeil
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårFeilkode
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårStegService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PeriodeMedOppfylteInngangsvilkår
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.Valideringsnøkkel
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.VilkårValideringFeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class InngangsvilkårStegServiceTest {
    private val vilkårMedlemskapService = mockk<VilkårMedlemskapService>(relaxed = true)
    private val vilkårDiagnoseService = mockk<VilkårDiagnoseService>(relaxed = true)
    private val pdlFaktagrunnlagService = mockk<PdlFaktagrunnlagService>(relaxed = true)
    private val behandlingStegService = mockk<BehandlingStegService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val beregnTilkjentYtelseService = mockk<BeregnTilkjentYtelseService>(relaxed = true)

    private val service =
        InngangsvilkårStegService(
            vilkårMedlemskapService,
            vilkårDiagnoseService,
            pdlFaktagrunnlagService,
            behandlingStegService,
            ansvarligSaksbehandlerService,
            endringshistorikkService,
            beregnTilkjentYtelseService,
        )

    private val behandlingId = UUID.randomUUID()

    init {
        medKomplettGrunnlag()
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns null
        every { behandlingStegService.registrerFullførteInngangsvilkår(any(), any()) } answers { utledStegEtterInngangsvilkår(secondArg()) }
    }

    @Test
    fun `fullførSteg kaster feil og skriver ingenting når behandling ikke er redigerbar`() {
        every { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR) } throws
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
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)
            pdlFaktagrunnlagService.hent(behandlingId)
            vilkårMedlemskapService.hentPerioder(behandlingId)
            vilkårDiagnoseService.hentPerioder(behandlingId)
            endringshistorikkService.registrerEndring(behandlingId, EndringType.INNGANGSVILKÅR_VURDERING_FULLFØRT, any())
        }
    }

    @Test
    fun `fullførSteg blokkerer videre behandling når personen har falsk identitet`() {
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns
            pdlFaktagrunnlagMedFalskIdentitet(erFalsk = true)

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(InngangsvilkårBlokkertFeil::class.java)
            .extracting { (it as InngangsvilkårBlokkertFeil).feilkode }
            .isEqualTo(InngangsvilkårFeilkode.FALSK_IDENTITET)

        verify(exactly = 0) { behandlingStegService.registrerFullførteInngangsvilkår(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg lar behandlingen gå videre når falskIdentitet har erFalsk false`() {
        every { pdlFaktagrunnlagService.hent(behandlingId) } returns
            pdlFaktagrunnlagMedFalskIdentitet(erFalsk = false)

        val inngangsvilkårResultat = service.fullførSteg(behandlingId)

        assertThat(inngangsvilkårResultat.nesteSteg).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
    }

    private fun pdlFaktagrunnlagMedFalskIdentitet(erFalsk: Boolean) =
        PdlFaktagrunnlag(
            hentetTidspunkt = LocalDateTime.of(2026, 1, 1, 12, 0),
            folkeregisterpersonstatus = emptyList(),
            bostedsadresse = emptyList(),
            statsborgerskap = emptyList(),
            opphold = emptyList(),
            innflyttingTilNorge = emptyList(),
            utflyttingFraNorge = emptyList(),
            oppholdsadresse = emptyList(),
            doedsfall = emptyList(),
            falskIdentitet =
                PdlFalskIdentitet(
                    behandlingId = behandlingId,
                    erFalsk = erFalsk,
                    metadata = PdlMetadata(historisk = false, master = "FREG"),
                ),
        )

    @Test
    fun `fullførSteg setter SATS_OG_TRYGDETID når det finnes perioder med oppfylte inngangsvilkår`() {
        service.fullførSteg(behandlingId)

        verify(exactly = 1) { behandlingStegService.registrerFullførteInngangsvilkår(behandlingId, harPerioderMedOppfylteInngangsvilkår = true) }
    }

    @Test
    fun `fullførSteg setter VEDTAK_OG_BREV når ingen perioder har oppfylte inngangsvilkår`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)

        service.fullførSteg(behandlingId)

        verify(exactly = 1) { behandlingStegService.registrerFullførteInngangsvilkår(behandlingId, harPerioderMedOppfylteInngangsvilkår = false) }
    }

    @Test
    fun `fullførSteg kaster VilkårValideringFeil og skriver ingenting når et inngangsvilkår mangler`() {
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns emptyList()

        assertThatThrownBy { service.fullførSteg(behandlingId) }
            .isInstanceOf(VilkårValideringFeil::class.java)
            .extracting { (it as VilkårValideringFeil).feil.map { feil -> feil.nøkkel } }
            .isEqualTo(listOf(Valideringsnøkkel.DIAGNOSE))

        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `fullførSteg returnerer periodene og går videre til sats og trygdetid`() {
        val resultat = service.fullførSteg(behandlingId)

        assertThat(resultat.perioder).containsExactly(
            PeriodeMedOppfylteInngangsvilkår(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
        )
        assertThat(resultat.nesteSteg).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
    }

    @Test
    fun `fullførSteg hopper til simulering når det ikke er rett i noen måned`() {
        medKomplettGrunnlag(medlemskapVurdering = Vurdering.NEI)

        val resultat = service.fullførSteg(behandlingId)

        assertThat(resultat.perioder).isEmpty()
        assertThat(resultat.nesteSteg).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
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
        verify { beregnTilkjentYtelseService.lagreTilkjentYtelseUtenAndeler(behandlingId) }
    }

    @Test
    fun `hentPerioder regner ut periodene uten å sjekke redigerbarhet eller skrive endringshistorikk`() {
        val perioder = service.hentPerioder(behandlingId)

        assertThat(perioder).containsExactly(
            PeriodeMedOppfylteInngangsvilkår(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
        )
        verify(exactly = 0) { behandlingStegService.validerKanEndreSteg(any(), any()) }
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
