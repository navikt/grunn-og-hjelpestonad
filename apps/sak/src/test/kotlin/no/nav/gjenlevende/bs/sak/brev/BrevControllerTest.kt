package no.nav.gjenlevende.bs.sak.brev

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.ApplicationLocalSetup
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmalDto
import no.nav.gjenlevende.bs.sak.brev.domain.InformasjonOmBrukerDto
import no.nav.gjenlevende.bs.sak.brev.domain.TekstbolkDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@WebMvcTest(BrevController::class)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
open class BrevControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var brevService: BrevService

    @MockkBean
    private lateinit var taskService: TaskService

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
}
