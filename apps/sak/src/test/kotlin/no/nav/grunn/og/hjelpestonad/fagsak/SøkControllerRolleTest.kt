package no.nav.grunn.og.hjelpestonad.fagsak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(SøkController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@Import(SøkControllerRolleTest.MetodesikkerhetConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("integrasjonstest")
class SøkControllerRolleTest {
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    open class MetodesikkerhetConfig

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var søkService: SøkService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    private val fagsakPersonId = UUID.randomUUID()
    private val url = "/api/sok/fagsak-person/$fagsakPersonId"

    private fun mockSøk() {
        every { søkService.søkMedFagsakPersonId(fagsakPersonId) } returns
            SøkeresultatResponse(
                navn = "Ola Nordmann",
                fødselsdato = null,
                personident = "12345678910",
                fagsakPersonId = fagsakPersonId,
                harFagsak = true,
            )
    }

    @Test
    @WithMockUser(roles = ["SAKSBEHANDLER"])
    fun `saksbehandler får søke`() {
        mockSøk()
        mockMvc.get(url).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["BESLUTTER"])
    fun `beslutter får søke`() {
        mockSøk()
        mockMvc.get(url).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser(roles = ["LESETILGANG"])
    fun `bruker med lesetilgang får søke`() {
        mockSøk()
        mockMvc.get(url).andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser
    fun `bruker uten rolle får ikke søke`() {
        mockMvc.get(url).andExpect { status { isForbidden() } }

        verify(exactly = 0) { tilgangService.validerTilgangTilFagsakPerson(any()) }
        verify(exactly = 0) { søkService.søkMedFagsakPersonId(any()) }
    }
}
