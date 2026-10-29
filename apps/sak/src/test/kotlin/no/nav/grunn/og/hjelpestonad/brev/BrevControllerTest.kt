package no.nav.grunn.og.hjelpestonad.brev

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.brev.domain.BrevRequest
import no.nav.grunn.og.hjelpestonad.brev.domain.BrevmalDto
import no.nav.grunn.og.hjelpestonad.brev.domain.InformasjonOmBrukerDto
import no.nav.grunn.og.hjelpestonad.brev.domain.TekstbolkDto
import no.nav.grunn.og.hjelpestonad.tilgangskontroll.TilgangService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertIs

@WebMvcTest(BrevController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
open class BrevControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var brevService: BrevService

    @MockkBean(relaxed = true)
    private lateinit var tilgangService: TilgangService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `opprettBrev returnerer 200 OK og behandlingId`() {
        val behandlingId = UUID.randomUUID()
        val brevRequest =
            BrevRequest(
                brevmal =
                    BrevmalDto(
                        tittel = "Test tittel",
                        informasjonOmBruker =
                            InformasjonOmBrukerDto(
                                navn = "Test Navn",
                                fnr = "12345678910",
                            ),
                        fastTekstAvslutning =
                            listOf(
                                TekstbolkDto(
                                    underoverskrift = "Avslutning",
                                    innhold = "Fast avslutningstekst",
                                ),
                            ),
                    ),
                fritekstbolker =
                    listOf(
                        TekstbolkDto(
                            underoverskrift = "Fritekst",
                            innhold = "Fritekstinnhold",
                        ),
                    ),
            )

        every {
            brevService.mellomlagreBrev(behandlingId, brevRequest)
        } returns Unit
        val responseJson =
            mockMvc
                .post("/api/brev/mellomlagre/$behandlingId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(brevRequest)
                }.andExpect {
                    status { isOk() }
                }

        verify(exactly = 1) {
            brevService.mellomlagreBrev(behandlingId, brevRequest)
        }
    }

    @Test
    fun `returnerer 400 ved ugyldig request body`() {
        val behandlingId = UUID.randomUUID()

        mockMvc
            .post("/api/brev/mellomlagre/$behandlingId") {
                contentType = MediaType.APPLICATION_JSON
                content = "ugyldig request"
            }.andExpect {
                status { isBadRequest() }
            }

        verify(exactly = 0) {
            brevService.mellomlagreBrev(any(), any())
        }
    }

    @Test
    fun `returnerer 400 ved ugyldig behandlingId i stien`() {
        val resultat =
            mockMvc
                .post("/api/brev/mellomlagre/ikke-en-uuid") {
                    contentType = MediaType.APPLICATION_JSON
                    content = "{}"
                }.andExpect {
                    status { isBadRequest() }
                }.andReturn()

        assertIs<MethodArgumentTypeMismatchException>(resultat.resolvedException)
        verify(exactly = 0) {
            brevService.mellomlagreBrev(any(), any())
        }
    }
}
