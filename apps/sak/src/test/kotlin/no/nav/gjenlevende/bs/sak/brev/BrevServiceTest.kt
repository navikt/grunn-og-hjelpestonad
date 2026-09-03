package no.nav.gjenlevende.bs.sak.brev

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmalDto
import no.nav.gjenlevende.bs.sak.brev.domain.InformasjonOmBrukerDto
import no.nav.gjenlevende.bs.sak.brev.domain.TekstbolkDto
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.ManglerTilgang
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.saksbehandler.EntraProxyClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test

class BrevServiceTest {
    private val brevRepository = mockk<BrevRepository>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val objectMapper = mockk<ObjectMapper>()
    private val entraProxyClient = mockk<EntraProxyClient>()
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val brevService = BrevService(brevRepository, behandlingService, objectMapper, entraProxyClient, endringshistorikkService, ansvarligSaksbehandlerService)

    @Test
    fun `mellomlagreBrev insert når brev ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        val brevRequest = gyldigBrevRequest()

        every { brevRepository.existsById(behandlingId) } returns false
        coEvery { brevRepository.insert(any()) } answers {
            firstArg<Brev>()
        }

        brevService.mellomlagreBrev(behandlingId, brevRequest)

        verify(exactly = 1) {
            brevRepository.insert(
                match {
                    it.behandlingId == behandlingId &&
                        it.brevJson == brevRequest
                },
            )
        }

        verify(exactly = 0) {
            brevRepository.update(any())
        }
    }

    @Test
    fun `mellomlagreBrev update når brev finnes`() {
        val behandlingId = UUID.randomUUID()
        val brevRequest = gyldigBrevRequest()

        every { brevRepository.existsById(behandlingId) } returns true
        coEvery { brevRepository.update(any()) } answers {
            firstArg<Brev>()
        }

        brevService.mellomlagreBrev(behandlingId, brevRequest)

        verify(exactly = 1) {
            brevRepository.update(
                match {
                    it.behandlingId == behandlingId &&
                        it.brevJson == brevRequest
                },
            )
        }

        verify(exactly = 0) {
            brevRepository.insert(any())
        }
    }

    @Test
    fun `hentBrev returnerer brev når det finnes`() {
        val behandlingId = UUID.randomUUID()
        val brev = Brev(behandlingId = behandlingId, brevJson = gyldigBrevRequest())

        every { brevRepository.findByIdOrNull(behandlingId) } returns brev
        val result = brevService.hentBrev(behandlingId)

        assertThat(result).isEqualTo(brev)
    }

    @Test
    fun `mellomlagreBrev kaster feil når behandling ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        val brevRequest = gyldigBrevRequest()

        every { behandlingService.validerBehandlingErRedigerbar(behandlingId) } throws Feil("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")

        assertThatThrownBy { brevService.mellomlagreBrev(behandlingId, brevRequest) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `mellomlagreBrev kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val brevRequest = gyldigBrevRequest()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { brevService.mellomlagreBrev(behandlingId, brevRequest) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `hentBrev returnerer null når brev ikke finnes`() {
        val behandlingId = UUID.randomUUID()

        every { brevRepository.findByIdOrNull(behandlingId) } returns null
        val result = brevService.hentBrev(behandlingId)

        assertThat(result).isNull()
    }

    private fun gyldigBrevRequest() =
        BrevRequest(
            brevmal =
                BrevmalDto(
                    tittel = "Tittel",
                    informasjonOmBruker =
                        InformasjonOmBrukerDto(
                            navn = "Navn",
                            fnr = "12345678910",
                        ),
                    fastTekstAvslutning =
                        listOf(
                            TekstbolkDto(
                                underoverskrift = "Avslutning",
                                innhold = "Innhold",
                            ),
                        ),
                ),
            fritekstbolker =
                listOf(
                    TekstbolkDto(
                        underoverskrift = "Fritekst",
                        innhold = "Mer innhold",
                    ),
                ),
        )
}
