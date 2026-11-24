package no.nav.grunn.og.hjelpestonad.vedtak

import efterlatte.prosessering.spring.TaskService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.beslutter.BeslutterService
import no.nav.grunn.og.hjelpestonad.beslutter.TotrinnskontrollService
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.beslutter.ÅrsakUnderkjent
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppdrag.IverksettOppdragTask
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.Oppgave
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import no.nav.grunn.og.hjelpestonad.steg.satsOgTrygdetid.SatsOgTrygdetidService
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.InngangsvilkårStegService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.AndelTilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.BeregnTilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelse
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import no.nav.grunn.og.hjelpestonad.vilkår.Vurdering
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifter
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.Regelverk
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class VedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val behandlingStegService = mockk<BehandlingStegService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val inngangsvilkårStegService = mockk<InngangsvilkårStegService>()
    private val vedtakService = VedtakService(vedtakRepository, endringshistorikkService, behandlingStegService, ansvarligSaksbehandlerService, tilkjentYtelseService, behandlingService, inngangsvilkårStegService)

    @BeforeEach
    fun settOppSaksbehandler() {
        every { vedtakRepository.insert(any<Vedtak>()) } answers { firstArg<Vedtak>() }
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"
        every { SikkerhetContext.hentSaksbehandlerEllerSystembruker() } returns "Z123456"
    }

    @AfterEach
    fun ryddSaksbehandler() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `lagrer innvilget vedtak fra beregningen uten barnetilsynsperioder`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns
            Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT)
        every { vedtakRepository.findByBehandlingId(behandlingId) } returns null
        every { inngangsvilkårStegService.hentPerioder(behandlingId) } returns listOf(mockk())
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns
            TilkjentYtelse(
                behandlingId = behandlingId,
                startmåned = YearMonth.of(2026, 4),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(beløp = 752.toBigDecimal(), fom = LocalDate.of(2026, 4, 1), tom = LocalDate.of(2029, 11, 30), kildeBehandlingId = behandlingId),
                    ),
            )

        vedtakService.lagreVedtakFraBehandlingsgrunnlag(behandlingId)

        verify {
            vedtakRepository.insert(
                match {
                    it.behandlingId == behandlingId && it.resultatType == ResultatType.INNVILGET &&
                        it.saksbehandlerIdent == "Z123456" && it.grunnstønadPerioder.isEmpty()
                },
            )
        }
    }

    @Test
    fun `lagrer avslag uten tilkjent ytelse når inngangsvilkårene ikke er oppfylt`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns
            Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT)
        every { vedtakRepository.findByBehandlingId(behandlingId) } returns null
        every { inngangsvilkårStegService.hentPerioder(behandlingId) } returns emptyList()
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null

        vedtakService.lagreVedtakFraBehandlingsgrunnlag(behandlingId)

        verify { vedtakRepository.insert(match { it.behandlingId == behandlingId && it.resultatType == ResultatType.AVSLÅTT }) }
    }

    @Test
    fun `avslag uten beregning består vedtaksresultatkontrollen`() {
        val behandlingId = UUID.randomUUID()
        every { vedtakRepository.findByBehandlingId(behandlingId) } returns
            Vedtak(behandlingId = behandlingId, resultatType = ResultatType.AVSLÅTT, grunnstønadPerioder = emptyList(), saksbehandlerIdent = "Z123456")
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null

        vedtakService.validerVedtaksresultatMotBeregning(behandlingId)
    }

    @Test
    fun `eksisterende vedtak bevares ved sending til beslutter`() {
        val behandlingId = UUID.randomUUID()
        every { vedtakRepository.findByBehandlingId(behandlingId) } returns
            Vedtak(behandlingId = behandlingId, resultatType = ResultatType.OPPHØR, opphørFom = YearMonth.of(2026, 4), grunnstønadPerioder = emptyList(), saksbehandlerIdent = "Z123456")

        vedtakService.lagreVedtakFraBehandlingsgrunnlag(behandlingId)

        verify(exactly = 0) { vedtakRepository.insert(any()) }
        verify(exactly = 0) { vedtakRepository.deleteByBehandlingId(any()) }
    }

    @Test
    fun `ufullført sats og trygdetid stopper vedtakslagring`() {
        val behandlingId = UUID.randomUUID()
        val behandlingStegService = BehandlingStegService(behandlingService)
        val vedtakService = VedtakService(vedtakRepository, endringshistorikkService, behandlingStegService, ansvarligSaksbehandlerService, tilkjentYtelseService, behandlingService, inngangsvilkårStegService)
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns
            Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)

        assertThatThrownBy { vedtakService.lagreVedtakFraBehandlingsgrunnlag(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fullfør «${BehandlingSteg.SATS_OG_TRYGDETID.visningsnavn}» før du går videre.")

        verify { vedtakRepository wasNot Called }
        verify { endringshistorikkService wasNot Called }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `ny sending etter underkjenning oppdaterer resultatet fra gjeldende grunnlag`(innvilgetEtterUnderkjenning: Boolean) {
        val behandlingId = UUID.randomUUID()
        var behandling = Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT, gjeldendeSteg = BehandlingSteg.VEDTAK_OG_BREV)
        var lagretVedtak: Vedtak? = null
        var harBeregnedeAndeler = !innvilgetEtterUnderkjenning
        val tilkjentYtelse =
            TilkjentYtelse(
                behandlingId = behandlingId,
                startmåned = YearMonth.of(2026, 4),
                andelerTilkjentYtelse =
                    setOf(
                        AndelTilkjentYtelse(
                            beløp = 752.toBigDecimal(),
                            fom = LocalDate.of(2026, 4, 1),
                            tom = LocalDate.of(2029, 11, 30),
                            kildeBehandlingId = behandlingId,
                        ),
                    ),
            )
        val oppgaveService = mockk<OppgaveService>(relaxed = true)
        val taskService = mockk<TaskService>(relaxed = true)
        val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
        val beslutterService = BeslutterService(behandlingService, behandlingStegService, mockk<BrevService>(relaxed = true), endringshistorikkService, oppgaveService, totrinnskontrollService, taskService, ansvarligSaksbehandlerService, vedtakService)
        every { behandlingService.hentBehandling(behandlingId) } answers { behandling }
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } answers { behandling }
        every { behandlingService.oppdaterBehandlingStatus(behandlingId, any()) } answers { behandling = behandling.copy(status = secondArg()) }
        every { behandlingService.oppdaterBehandlingResultat(behandlingId, any()) } answers { behandling = behandling.copy(resultat = secondArg()) }
        every { vedtakRepository.findByBehandlingId(behandlingId) } answers { lagretVedtak }
        every { vedtakRepository.insert(any<Vedtak>()) } answers { firstArg<Vedtak>().also { lagretVedtak = it } }
        every { vedtakRepository.update(any<Vedtak>()) } answers { firstArg<Vedtak>().also { lagretVedtak = it } }
        every { oppgaveService.hentOppgaveForBehandling(behandlingId) } returns Oppgave(behandlingId = behandlingId, gsakOppgaveId = 1L, type = OppgavetypeGRU.BEH_SAK.name)
        every { totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId) } returns "Z123456"
        every { inngangsvilkårStegService.hentPerioder(behandlingId) } answers { if (harBeregnedeAndeler) listOf(mockk()) else emptyList() }
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } answers { if (harBeregnedeAndeler) tilkjentYtelse else null }

        beslutterService.sendTilBeslutter(behandlingId)
        val opprinneligVedtak = requireNotNull(lagretVedtak)
        assertThat(opprinneligVedtak.resultatType).isEqualTo(if (innvilgetEtterUnderkjenning) ResultatType.AVSLÅTT else ResultatType.INNVILGET)
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = false, årsakUnderkjent = ÅrsakUnderkjent.VILKÅR, begrunnelse = "Vilkårene må vurderes på nytt"))
        harBeregnedeAndeler = innvilgetEtterUnderkjenning

        beslutterService.sendTilBeslutter(behandlingId)
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))

        assertThat(lagretVedtak?.id).isEqualTo(opprinneligVedtak.id)
        assertThat(lagretVedtak?.resultatType).isEqualTo(if (innvilgetEtterUnderkjenning) ResultatType.INNVILGET else ResultatType.AVSLÅTT)
        assertThat(behandling.resultat).isEqualTo(if (innvilgetEtterUnderkjenning) BehandlingResultat.INNVILGET else BehandlingResultat.AVSLÅTT)
        assertThat(behandling.status).isEqualTo(if (innvilgetEtterUnderkjenning) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.FERDIGSTILT)
        verify(exactly = if (innvilgetEtterUnderkjenning) 1 else 0) {
            taskService.opprett(IverksettOppdragTask.TYPE, IverksettOppdragTask.IverksettOppdragTaskData(behandlingId, "Z123456"), any(), any())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `sending og godkjenning lagrer vedtak og starter oppdrag bare ved innvilgelse`(
        harBeregnedeAndeler: Boolean,
    ) {
        val behandlingId = UUID.randomUUID()
        var behandling = Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT, gjeldendeSteg = BehandlingSteg.VEDTAK_OG_BREV)
        var lagretVedtak: Vedtak? = null
        val oppgaveService = mockk<OppgaveService>(relaxed = true)
        val taskService = mockk<TaskService>(relaxed = true)
        val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
        val beslutterService = BeslutterService(behandlingService, behandlingStegService, mockk<BrevService>(relaxed = true), endringshistorikkService, oppgaveService, totrinnskontrollService, taskService, ansvarligSaksbehandlerService, vedtakService)
        every { behandlingService.hentBehandling(behandlingId) } answers { behandling }
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } answers { behandling }
        every { behandlingService.oppdaterBehandlingStatus(behandlingId, any()) } answers { behandling = behandling.copy(status = secondArg()) }
        every { behandlingService.oppdaterBehandlingResultat(behandlingId, any()) } answers { behandling = behandling.copy(resultat = secondArg()) }
        every { vedtakRepository.findByBehandlingId(behandlingId) } answers { lagretVedtak }
        every { vedtakRepository.insert(any<Vedtak>()) } answers { firstArg<Vedtak>().also { lagretVedtak = it } }
        every { oppgaveService.hentOppgaveForBehandling(behandlingId) } returns Oppgave(behandlingId = behandlingId, gsakOppgaveId = 1L, type = OppgavetypeGRU.BEH_SAK.name)
        every { totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId) } returns "Z123456"
        every { inngangsvilkårStegService.hentPerioder(behandlingId) } returns if (harBeregnedeAndeler) listOf(mockk()) else emptyList()
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns
            if (harBeregnedeAndeler) {
                TilkjentYtelse(
                    behandlingId = behandlingId,
                    startmåned = YearMonth.of(2026, 4),
                    andelerTilkjentYtelse =
                        setOf(
                            AndelTilkjentYtelse(beløp = 752.toBigDecimal(), fom = LocalDate.of(2026, 4, 1), tom = LocalDate.of(2029, 11, 30), kildeBehandlingId = behandlingId),
                        ),
                )
            } else {
                null
            }

        beslutterService.sendTilBeslutter(behandlingId)
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))

        verify { behandlingService.oppdaterBehandlingResultat(behandlingId, if (harBeregnedeAndeler) BehandlingResultat.INNVILGET else BehandlingResultat.AVSLÅTT) }
        verify { behandlingService.oppdaterBehandlingStatus(behandlingId, if (harBeregnedeAndeler) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.FERDIGSTILT) }
        if (harBeregnedeAndeler) {
            verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.FERDIGSTILT) }
        }
        verify(exactly = if (harBeregnedeAndeler) 1 else 0) {
            taskService.opprett(IverksettOppdragTask.TYPE, IverksettOppdragTask.IverksettOppdragTaskData(behandlingId, "Z123456"), any(), any())
        }
    }

    @Test
    fun `avslag på ekstrautgifter kan beregnes sendes til beslutter og godkjennes uten utbetaling`() {
        val behandlingId = UUID.randomUUID()
        var behandling = Behandling(id = behandlingId, fagsakId = UUID.randomUUID(), status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)
        var lagretVedtak: Vedtak? = null
        val oppgaveService = mockk<OppgaveService>(relaxed = true)
        val taskService = mockk<TaskService>(relaxed = true)
        val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
        val behandlingStegService = BehandlingStegService(behandlingService)
        val vedtakService = VedtakService(vedtakRepository, endringshistorikkService, behandlingStegService, ansvarligSaksbehandlerService, tilkjentYtelseService, behandlingService, inngangsvilkårStegService)
        val beslutterService = BeslutterService(behandlingService, behandlingStegService, mockk<BrevService>(relaxed = true), endringshistorikkService, oppgaveService, totrinnskontrollService, taskService, ansvarligSaksbehandlerService, vedtakService)
        every { behandlingService.hentBehandling(behandlingId) } answers { behandling }
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } answers { behandling }
        every { behandlingService.oppdaterBehandlingStatus(behandlingId, any()) } answers { behandling = behandling.copy(status = secondArg()) }
        every { behandlingService.oppdaterBehandlingResultat(behandlingId, any()) } answers { behandling = behandling.copy(resultat = secondArg()) }
        every { vedtakRepository.findByBehandlingId(behandlingId) } answers { lagretVedtak }
        every { vedtakRepository.insert(any<Vedtak>()) } answers { firstArg<Vedtak>().also { lagretVedtak = it } }
        every { oppgaveService.hentOppgaveForBehandling(behandlingId) } returns Oppgave(behandlingId = behandlingId, gsakOppgaveId = 1L, type = OppgavetypeGRU.BEH_SAK.name)
        every { inngangsvilkårStegService.hentPerioder(behandlingId) } returns listOf(mockk())
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null
        every { behandlingService.oppdater(any()) } answers { firstArg<Behandling>().also { behandling = it } }
        val vilkårMedlemskapService = mockk<VilkårMedlemskapService>()
        val vilkårDiagnoseService = mockk<VilkårDiagnoseService>()
        val vilkårEkstrautgifterService = mockk<VilkårEkstrautgifterService>()
        val fraOgMedDato = LocalDate.of(2026, 3, 1)
        every { vilkårMedlemskapService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårMedlemskap(behandlingId = behandlingId, vurdering = Vurdering.JA, regelverk = Regelverk.NASJONALE_REGLER, fraOgMedDato = fraOgMedDato),
            )
        every { vilkårDiagnoseService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårDiagnose(behandlingId = behandlingId, vurdering = Vurdering.JA, kode = "E109", tekst = "Testdiagnose", erYrkesskade = false, fraOgMedDato = fraOgMedDato),
            )
        every { vilkårEkstrautgifterService.hentPerioder(behandlingId) } returns
            listOf(
                VilkårEkstrautgifter(behandlingId = behandlingId, vurdering = Vurdering.NEI, sats = null, fraOgMedDato = fraOgMedDato),
            )
        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(behandlingService, tilkjentYtelseService, vilkårMedlemskapService, vilkårDiagnoseService, vilkårEkstrautgifterService, mockk(relaxed = true))
        val satsOgTrygdetidService = SatsOgTrygdetidService(behandlingStegService, beregnTilkjentYtelseService, ansvarligSaksbehandlerService, endringshistorikkService)

        satsOgTrygdetidService.fullførSteg(behandlingId)

        assertThat(behandling.gjeldendeSteg).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
        verify { tilkjentYtelseService.slettTilkjentYtelseHvisFinnes(behandlingId) }
        verify(exactly = 0) { tilkjentYtelseService.lagre(any()) }
        beslutterService.sendTilBeslutter(behandlingId)
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))

        assertThat(behandling.resultat).isEqualTo(BehandlingResultat.AVSLÅTT)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(lagretVedtak?.resultatType).isEqualTo(ResultatType.AVSLÅTT)
        verify(exactly = 0) {
            taskService.opprett(IverksettOppdragTask.TYPE, IverksettOppdragTask.IverksettOppdragTaskData(behandlingId, "Z123456"), any(), any())
        }
    }

    @Test
    fun `manglende vedtak eller slettet beregning stopper førstegangskontrollen`() {
        val behandlingId = UUID.randomUUID()
        every { vedtakRepository.findByBehandlingId(behandlingId) } returns null
        assertFailsWith<Feil> { vedtakService.validerVedtaksresultatMotBeregning(behandlingId) }

        every { vedtakRepository.findByBehandlingId(behandlingId) } returns Vedtak(behandlingId = behandlingId, resultatType = ResultatType.INNVILGET, grunnstønadPerioder = emptyList(), saksbehandlerIdent = "Z123456", opprettetAv = "Z123456")
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null
        assertFailsWith<Feil> { vedtakService.validerVedtaksresultatMotBeregning(behandlingId) }
    }

    @Test
    fun `førstegangsvedtak må samsvare med om beregningen har andeler`() {
        val behandlingId = UUID.randomUUID()
        val andelTilkjentYtelse = AndelTilkjentYtelse(beløp = 752.toBigDecimal(), fom = LocalDate.of(2026, 4, 1), tom = LocalDate.of(2029, 11, 30), kildeBehandlingId = behandlingId)
        for (resultatType in listOf(ResultatType.INNVILGET, ResultatType.AVSLÅTT)) {
            every { vedtakRepository.findByBehandlingId(behandlingId) } returns Vedtak(behandlingId = behandlingId, resultatType = resultatType, grunnstønadPerioder = emptyList(), saksbehandlerIdent = "Z123456", opprettetAv = "Z123456")
            for (harBeregnedeAndeler in listOf(true, false)) {
                every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns TilkjentYtelse(behandlingId = behandlingId, startmåned = YearMonth.of(2026, 4), andelerTilkjentYtelse = if (harBeregnedeAndeler) setOf(andelTilkjentYtelse) else emptySet())
                if ((resultatType == ResultatType.INNVILGET) == harBeregnedeAndeler) {
                    vedtakService.validerVedtaksresultatMotBeregning(behandlingId)
                } else {
                    assertFailsWith<UgyldigInput> { vedtakService.validerVedtaksresultatMotBeregning(behandlingId) }
                }
            }
        }
    }

    @Test
    fun `vedtak kan ikke opprette utbetaling uten fullført beregning`() {
        val behandlingId = UUID.randomUUID()
        every { tilkjentYtelseService.hentTilkjentYtelse(behandlingId) } returns null
        assertThatThrownBy {
            vedtakService.lagreVedtak(VedtakRequest(resultatType = ResultatType.INNVILGET, begrunnelse = "Test", grunnstønadPerioder = emptyList()), behandlingId)
        }.isInstanceOf(Feil::class.java).hasMessageContaining("Fullfør sats og trygdetid")
        verify(exactly = 0) { vedtakRepository.insert(any()) }
    }

    @Test
    fun `lagreVedtak kaster feil når behandling ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        val vedtakRequest =
            VedtakRequest(
                resultatType = ResultatType.INNVILGET,
                begrunnelse = "Test",
                grunnstønadPerioder = emptyList(),
            )

        every { behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { vedtakService.lagreVedtak(vedtakRequest, behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `lagreVedtak kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val vedtakRequest =
            VedtakRequest(
                resultatType = ResultatType.INNVILGET,
                begrunnelse = "Test",
                grunnstønadPerioder = emptyList(),
            )

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { vedtakService.lagreVedtak(vedtakRequest, behandlingId) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `slettVedtakHvisFinnes kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { vedtakService.slettVedtakHvisFinnes(behandlingId) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }
}
