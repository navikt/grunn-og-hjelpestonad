package no.nav.grunn.og.hjelpestonad.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.brev.domain.Brevmottaker
import no.nav.grunn.og.hjelpestonad.brev.domain.BrevmottakerRolle
import no.nav.grunn.og.hjelpestonad.brev.domain.MottakerType
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.util.UUID
import kotlin.test.Test

class BrevmottakerServiceTest {
    private val brevmottakerRepository = mockk<BrevmottakerRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val brevmottakerService = BrevmottakerService(brevmottakerRepository, behandlingService, ansvarligSaksbehandlerService)

    @Test
    fun `oppdaterBrevmottakere kaster feil når behandling ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        val brevmottakere =
            listOf(
                Brevmottaker(
                    behandlingId = behandlingId,
                    personRolle = BrevmottakerRolle.BRUKER,
                    mottakerType = MottakerType.PERSON,
                    personident = "12345678910",
                ),
            )

        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { brevmottakerService.oppdaterBrevmottakere(behandlingId, brevmottakere) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `oppdaterBrevmottakere kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val brevmottakere =
            listOf(
                Brevmottaker(
                    behandlingId = behandlingId,
                    personRolle = BrevmottakerRolle.BRUKER,
                    mottakerType = MottakerType.PERSON,
                    personident = "12345678910",
                ),
            )

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { brevmottakerService.oppdaterBrevmottakere(behandlingId, brevmottakere) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }
}
