package no.nav.grunn.og.hjelpestonad.behandling.årsak

import io.mockk.every
import io.mockk.mockk
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class ÅrsakBehandlingServiceTest {
    private val årsakBehandlingRepository = mockk<ÅrsakBehandlingRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val årsakBehandlingService = ÅrsakBehandlingService(årsakBehandlingRepository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

    @Test
    fun `lagreÅrsakForBehandling kaster feil når behandling ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        val request =
            ÅrsakBehandlingRequest(
                kravdato = LocalDate.now(),
                årsak = Årsak.SØKNAD,
                beskrivelse = "Test",
            )

        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `lagreÅrsakForBehandling kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val request =
            ÅrsakBehandlingRequest(
                kravdato = LocalDate.now(),
                årsak = Årsak.SØKNAD,
                beskrivelse = "Test",
            )

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }
}
