package no.nav.grunn.og.hjelpestonad.fagsak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.ApplicationLocalSetup
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThat
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
import java.util.UUID

@WebMvcTest(
    FagsakController::class,
)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
open class FagsakControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var fagsakService: FagsakService

    @MockkBean
    private lateinit var fagsakPersonService: FagsakPersonService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal returnere fagsak når person finnes`() {
        val personident = "12345678910"
        val stønadstype = StønadType.BARNETILSYN
        val fagsakRequest =
            FagsakRequest(
                personident = personident,
                fagsakPersonId = null,
                stønadstype = stønadstype,
            )

        val forventetFagsak =
            FagsakResponse(
                personident = personident,
                stønadstype = stønadstype,
                id = UUID.randomUUID(),
                fagsakPersonId = UUID.randomUUID(),
                eksternId = 1L,
            )

        val request =
            FagsakRequest(
                personident = personident,
                fagsakPersonId = null,
                stønadstype = stønadstype,
            )

        every {
            fagsakService.hentEllerOpprettFagsak(request)
        } returns forventetFagsak

        val responseJson =
            mockMvc
                .post("/api/fagsak") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(fagsakRequest)
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }.andReturn()
                .response.contentAsString

        val fagsakResponse: FagsakResponse = objectMapper.readValue(responseJson)
        assertThat(fagsakResponse.personident).isEqualTo(personident)
        assertThat(fagsakResponse.stønadstype).isEqualTo(stønadstype)

        verify(exactly = 1) {
            fagsakService.hentEllerOpprettFagsak(request)
        }
    }

    @Test
    fun `skal returnere fagsak når fagsakPersonId finnes`() {
        val fagsakPersonId = UUID.randomUUID()
        val personident = "12345678910"
        val stønadstype = StønadType.BARNETILSYN
        val fagsakRequest =
            FagsakRequest(
                personident = null,
                fagsakPersonId = fagsakPersonId,
                stønadstype = stønadstype,
            )

        val forventetFagsak =
            FagsakResponse(
                personident = personident,
                stønadstype = stønadstype,
                id = UUID.randomUUID(),
                fagsakPersonId = fagsakPersonId,
                eksternId = 1L,
            )

        val request =
            FagsakRequest(
                personident = null,
                fagsakPersonId = fagsakPersonId,
                stønadstype = stønadstype,
            )

        every {
            fagsakService.hentEllerOpprettFagsak(request)
        } returns forventetFagsak

        every { fagsakPersonService.hentAktivIdent(any()) } returns personident

        val responseJson =
            mockMvc
                .post("/api/fagsak") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(fagsakRequest)
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }.andReturn()
                .response.contentAsString

        val fagsakResponse: FagsakResponse = objectMapper.readValue(responseJson)
        assertThat(fagsakResponse.fagsakPersonId).isEqualTo(fagsakPersonId)
        assertThat(fagsakResponse.stønadstype).isEqualTo(stønadstype)

        verify(exactly = 1) {
            fagsakService.hentEllerOpprettFagsak(request)
        }
    }

    @Test
    fun `skal kaste feil når hverken personident eller fagsakPersonId er oppgitt`() {
        val ugyldigRequest =
            FagsakRequest(
                personident = null,
                fagsakPersonId = null,
                stønadstype = StønadType.BARNETILSYN,
            )

        every {
            fagsakService.hentEllerOpprettFagsak(ugyldigRequest)
        } throws Feil("Må oppgi enten personident eller fagsakPersonId")

        mockMvc
            .post("/api/fagsak") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(ugyldigRequest)
            }.andExpect {
                status { isInternalServerError() }
            }
    }

    @Test
    fun `skal godta request med kun fagsakPersonId`() {
        val fagsakPersonId = UUID.randomUUID()

        val request =
            FagsakRequest(
                personident = null,
                fagsakPersonId = fagsakPersonId,
                stønadstype = StønadType.BARNETILSYN,
            )

        every {
            fagsakService.hentEllerOpprettFagsak(any())
        } returns
            FagsakResponse(
                personident = "12345678910",
                stønadstype = StønadType.BARNETILSYN,
                id = UUID.randomUUID(),
                fagsakPersonId = fagsakPersonId,
                eksternId = 1L,
            )

        every { fagsakPersonService.hentAktivIdent(any()) } returns "12345678910"

        mockMvc
            .post("/api/fagsak") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
            }
    }
}
