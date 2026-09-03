package no.nav.gjenlevende.bs.sak.pdl

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.gjenlevende.bs.sak.ApplicationLocalSetup
import org.assertj.core.api.Assertions
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
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(
    PdlController::class,
)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
open class PdlControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var pdlService: PdlService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `hentPerson returnerer 200 OK med navn når person finnes`() {
        val fagsakPersonId = UUID.randomUUID()
        val hentPersonRequest = HentPersonRequest(fagsakPersonId)

        every {
            pdlService.hentPersonMedFagsakPersonId(fagsakPersonId)
        } returns Person(Navn("fornavn", null, "etternavn"), LocalDate.of(1990, 1, 15))
        val responseJson =
            mockMvc
                .post("/api/pdl/person") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(hentPersonRequest)
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }.andReturn()
                .response.contentAsString
        val response = objectMapper.readValue<Person>(responseJson)
        Assertions.assertThat(response.navn.fornavn).isEqualTo("fornavn")
        Assertions.assertThat(response.navn.etternavn).isEqualTo("etternavn")
        Assertions.assertThat(response.foedselsdato).isEqualTo("1990-01-15")

        verify(exactly = 1) {
            pdlService.hentPersonMedFagsakPersonId(fagsakPersonId)
        }
    }

    @Test
    fun `returnerer 400 ved ugyldig request`() {
        mockMvc
            .post("/api/pdl/person") {
                contentType = MediaType.APPLICATION_JSON
                content = "ugyldig request"
            }.andExpect {
                status { isBadRequest() }
            }

        verify(exactly = 0) {
            pdlService.hentPersonMedFagsakPersonId(any())
        }
    }
}
