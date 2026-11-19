package no.nav.grunn.og.hjelpestonad.behandling.årsak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test

class ÅrsakBehandlingServiceTest {
    private val årsakBehandlingRepository = mockk<ÅrsakBehandlingRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val årsakBehandlingService = ÅrsakBehandlingService(årsakBehandlingRepository, behandlingService, endringshistorikkService, ansvarligSaksbehandlerService)

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
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `lagreÅrsakForBehandling kaster og skriver ingenting når behandlingen er FATTER_VEDTAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.ÅRSAK) } throws
            Feil(
                melding = "Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK",
                httpStatus = HttpStatus.BAD_REQUEST,
            )

        assertThatThrownBy { årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")

        verify(exactly = 0) { årsakBehandlingRepository.findById(any()) }
        verify(exactly = 0) { årsakBehandlingRepository.insert(any<ÅrsakBehandling>()) }
        verify(exactly = 0) { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) }
        verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
        verify(exactly = 0) { behandlingService.settNåværendeSteg(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
    }

    @Test
    fun `lagreÅrsakForBehandling setter status UTREDES også når årsaken allerede finnes`() {
        val behandlingId = UUID.randomUUID()
        val request =
            ÅrsakBehandlingRequest(
                kravdato = LocalDate.now(),
                årsak = Årsak.SØKNAD,
                beskrivelse = "Test",
            )
        val eksisterendeÅrsak =
            ÅrsakBehandling(
                behandlingId = behandlingId,
                kravdato = LocalDate.now().minusDays(1),
                årsak = Årsak.ANNET,
            )
        every { årsakBehandlingRepository.findById(behandlingId) } returns Optional.of(eksisterendeÅrsak)
        every { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) } answers { firstArg<ÅrsakBehandling>() }

        årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request)

        verify(exactly = 1) {
            behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.UTREDES)
        }
    }

    @Test
    fun `ny årsak setter INNGANGSVILKÅR`() {
        val behandlingId = UUID.randomUUID()
        every { årsakBehandlingRepository.findById(behandlingId) } returns Optional.empty()
        every { årsakBehandlingRepository.insert(any<ÅrsakBehandling>()) } answers { firstArg<ÅrsakBehandling>() }

        årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request())

        verify(exactly = 1) { behandlingService.settNåværendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR) }
    }

    @Test
    fun `endret årsak setter INNGANGSVILKÅR når steget står senere`() {
        val behandlingId = UUID.randomUUID()
        every { årsakBehandlingRepository.findById(behandlingId) } returns Optional.of(eksisterendeÅrsak(behandlingId))
        every { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) } answers { firstArg<ÅrsakBehandling>() }

        årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request(årsak = Årsak.NYE_OPPLYSNINGER))

        verify(exactly = 1) { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) }
        verify(exactly = 1) { behandlingService.settNåværendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR) }
    }

    @Test
    fun `uendret årsak lagres ikke når steget står senere`() {
        val behandlingId = UUID.randomUUID()
        every { årsakBehandlingRepository.findById(behandlingId) } returns Optional.of(eksisterendeÅrsak(behandlingId))

        val resultat = årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request())

        assertThat(resultat).isEqualTo(eksisterendeÅrsak(behandlingId))
        verify(exactly = 0) { årsakBehandlingRepository.insert(any<ÅrsakBehandling>()) }
        verify(exactly = 0) { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) }
        verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
        verify(exactly = 0) { behandlingService.settNåværendeSteg(any(), any()) }
    }

    @Test
    fun `uendret årsak lagres ikke og setter ikke steg, også når steget står på ÅRSAK`() {
        val behandlingId = UUID.randomUUID()
        every { årsakBehandlingRepository.findById(behandlingId) } returns Optional.of(eksisterendeÅrsak(behandlingId))

        val resultat = årsakBehandlingService.lagreÅrsakForBehandling(behandlingId, request())

        assertThat(resultat).isEqualTo(eksisterendeÅrsak(behandlingId))
        verify(exactly = 0) { årsakBehandlingRepository.update(any<ÅrsakBehandling>()) }
        verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any(), any()) }
        verify(exactly = 0) { behandlingService.settNåværendeSteg(any(), any()) }
    }

    private fun request(
        kravdato: LocalDate = LocalDate.of(2026, 10, 1),
        årsak: Årsak = Årsak.SØKNAD,
        beskrivelse: String = "Test",
    ) = ÅrsakBehandlingRequest(kravdato = kravdato, årsak = årsak, beskrivelse = beskrivelse)

    private fun eksisterendeÅrsak(behandlingId: UUID) =
        ÅrsakBehandling(
            behandlingId = behandlingId,
            kravdato = LocalDate.of(2026, 10, 1),
            årsak = Årsak.SØKNAD,
            beskrivelse = "Test",
        )
}
