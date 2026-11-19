package no.nav.grunn.og.hjelpestonad.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.util.UUID
import kotlin.test.Test

class VedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val behandlingStegService = mockk<BehandlingStegService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val vedtakService = VedtakService(vedtakRepository, endringshistorikkService, behandlingStegService, ansvarligSaksbehandlerService, tilkjentYtelseService)

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
