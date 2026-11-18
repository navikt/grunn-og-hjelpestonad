package no.nav.grunn.og.hjelpestonad.steg.satsogtrygdetid

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.oppdrag.Oppdragsgrunnlag
import no.nav.grunn.og.hjelpestonad.oppdrag.lagUtbetalingsoppdrag
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseUtleder
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.UtledningAvTilkjentYtelseFeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.Utledningsfeil
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.Valideringsnøkkel
import no.nav.grunn.og.hjelpestonad.trygdetid.Trygdetid
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidKilde
import no.nav.grunn.og.hjelpestonad.trygdetid.TrygdetidService
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Ekstrautgift
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Hjemmelbokstav
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.Sats
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertFailsWith

class SatsOgTrygdetidStegServiceTest {
    private val behandlingId = UUID.randomUUID()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val medlemskapService = mockk<VilkårMedlemskapService>()
    private val diagnoseService = mockk<VilkårDiagnoseService>()
    private val ekstrautgifterService = mockk<VilkårEkstrautgifterService>()
    private val trygdetidService = mockk<TrygdetidService>()
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val service = SatsOgTrygdetidStegService(behandlingService, tilkjentYtelseService, medlemskapService, diagnoseService, ekstrautgifterService, trygdetidService, ansvarligSaksbehandlerService, mockk<EndringshistorikkService>(relaxed = true), Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC))
    private val fraOgMedDato = LocalDate.of(2026, 3, 1)
    private val behandling = Behandling(fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT)

    init {
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling
        every { medlemskapService.hentPerioder(behandlingId) } returns listOf(VilkårMedlemskap(behandlingId = behandlingId, vurdering = Vurdering.JA, regelverk = Regelverk.NASJONALE_REGLER, fraOgMedDato = fraOgMedDato))
        every { diagnoseService.hentPerioder(behandlingId) } returns listOf(VilkårDiagnose(behandlingId = behandlingId, vurdering = Vurdering.JA, kode = "E109", tekst = "Testdiagnose", erYrkesskade = false, fraOgMedDato = fraOgMedDato))
        every { ekstrautgifterService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårEkstrautgifter(behandlingId = behandlingId, vurdering = Vurdering.JA, sats = Sats.SATS_2, fraOgMedDato = fraOgMedDato)
                    .medEkstrautgifter(listOf(Ekstrautgift(Hjemmelbokstav.A, setOf("E109")))),
            )
        every { trygdetidService.hentPerioder(behandlingId) } returns listOf(Trygdetid(behandlingId = behandlingId, teller = 29, kilde = TrygdetidKilde.ALDERSPENSJON, fraOgMedDato = fraOgMedDato))
    }

    @Test
    fun `fullføring lagrer faktisk beregning med desimaler og treårsgrense`() {
        val tilkjentYtelse = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.lagre(capture(tilkjentYtelse)) } returns Unit
        service.fullførSteg(behandlingId)
        assertThat(tilkjentYtelse.captured.startmåned).isEqualTo(YearMonth.of(2026, 4))
        val andelTilkjentYtelse = tilkjentYtelse.captured.andelerTilkjentYtelse.single()
        assertThat(andelTilkjentYtelse.beløp).isEqualByComparingTo("830.85")
        assertThat(andelTilkjentYtelse.tom).isEqualTo(LocalDate.of(2029, 11, 30))
        verifyOrder {
            behandlingService.validerBehandlingErRedigerbar(behandlingId)
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            medlemskapService.hentPerioder(behandlingId)
            tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId)
            tilkjentYtelseService.lagre(any())
        }
        val beregnetOppdrag =
            lagUtbetalingsoppdrag(
                Oppdragsgrunnlag(
                    behandlingId = behandlingId,
                    eksternBehandlingId = 1,
                    eksternFagsakId = 2,
                    personIdent = "12345678910",
                    saksbehandlerId = "Z123456",
                    vedtaksdato = LocalDate.of(2026, 11, 17),
                    andelerTilkjentYtelse = tilkjentYtelse.captured.andelerTilkjentYtelse.toList(),
                ),
            )
        assertThat(
            beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .sats,
        ).isEqualByComparingTo("831")
        assertThat(
            beregnetOppdrag.utbetalingsoppdrag.utbetalingsperiode
                .single()
                .vedtakdatoTom,
        ).isEqualTo(LocalDate.of(2029, 11, 30))
    }

    @Test
    fun `hull i trygdetid stopper fullføring uten å endre lagret beregning`() {
        every { trygdetidService.hentPerioder(behandlingId) } returns emptyList()
        assertFailsWith<UtledningAvTilkjentYtelseFeil> { service.fullførSteg(behandlingId) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
        verify(exactly = 0) { tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(any()) }
    }

    @Test
    fun `revurdering uten andeler beholder forrige startmåned`() {
        val forrigeBehandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling.copy(forrigeBehandlingId = forrigeBehandlingId)
        every { tilkjentYtelseService.hentTilkjentYtelse(forrigeBehandlingId) } returns TilkjentYtelse(behandlingId = forrigeBehandlingId, startmåned = YearMonth.of(2025, 1), andelerTilkjentYtelse = emptySet())
        val tilkjentYtelse = slot<TilkjentYtelse>()
        every { tilkjentYtelseService.lagre(capture(tilkjentYtelse)) } returns Unit
        service.lagreTilkjentYtelseUtenAndeler(behandlingId)
        assertThat(tilkjentYtelse.captured.startmåned).isEqualTo(YearMonth.of(2025, 1))
        assertThat(tilkjentYtelse.captured.andelerTilkjentYtelse).isEmpty()
    }

    @Test
    fun `førstegangsbehandling uten andeler sletter tilkjent ytelse uten å lagre`() {
        service.lagreTilkjentYtelseUtenAndeler(behandlingId)

        verify(exactly = 1) { tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
    }

    @Test
    fun `fullføring med ikke oppfylte inngangsvilkår sletter tilkjent ytelse uten å lagre`() {
        every { medlemskapService.hentPerioder(behandlingId) } returns
            listOf(VilkårMedlemskap(behandlingId = behandlingId, vurdering = Vurdering.NEI, regelverk = Regelverk.NASJONALE_REGLER, fraOgMedDato = fraOgMedDato))
        every { diagnoseService.hentPerioder(behandlingId) } returns
            listOf(VilkårDiagnose(behandlingId = behandlingId, vurdering = Vurdering.NEI, kode = "E109", tekst = "Testdiagnose", erYrkesskade = false, fraOgMedDato = fraOgMedDato))
        every { ekstrautgifterService.hentPerioder(behandlingId) } returns
            listOf(VilkårEkstrautgifter(behandlingId = behandlingId, vurdering = Vurdering.NEI, sats = null, fraOgMedDato = fraOgMedDato))

        service.fullførSteg(behandlingId)

        verify(exactly = 1) { tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
    }

    @Test
    fun `diagnose endret etter lagring av ekstrautgifter stopper beregningen`() {
        every { ekstrautgifterService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårEkstrautgifter(behandlingId = behandlingId, vurdering = Vurdering.JA, sats = Sats.SATS_2, fraOgMedDato = fraOgMedDato)
                    .medEkstrautgifter(listOf(Ekstrautgift(Hjemmelbokstav.A, setOf("G35")))),
            )
        assertFailsWith<UgyldigInput> { service.fullførSteg(behandlingId) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
    }

    @Test
    fun `manglende ansvar stopper før grunnlaget leses`() {
        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws IllegalStateException("Ikke ansvarlig")
        assertFailsWith<IllegalStateException> { service.fullførSteg(behandlingId) }
        verify(exactly = 0) { medlemskapService.hentPerioder(any()) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
    }
}
