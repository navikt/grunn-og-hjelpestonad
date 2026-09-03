package no.nav.gjenlevende.bs.sak.vedtak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.gjenlevende.bs.sak.ApplicationLocalSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(
    VedtakController::class,
)
@ContextConfiguration(classes = [ApplicationLocalSetup::class])
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integrasjonstest")
class VedtakControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var vedtakService: VedtakService

    @MockkBean
    private lateinit var gjeldendeVedtakService: GjeldendeVedtakService

    @Test
    fun `Skal lagre vedtak fra json`() {
        val json = """{
  "resultatType": "INNVILGET",
  "begrunnelse": "Mock begrunnelse",
  "barnetilsynperioder": [
    {
      "behandlingId": "86460749-53d4-481b-8909-7286efc7eaad",
      "datoFra": "2024-01",
      "datoTil": "2024-01",
      "utgifter": 1000,
      "barn": [
        "b1e1d2c3-1111-2222-3333-444455556666"
      ],
      "periodetype": "ORDINÆR",
      "aktivitetstype": "I_ARBEID"
    }
  ],
  "saksbehandlerIdent": "mockIdent",
  "opprettetTid": "2026-01-21T09:52:13.329Z",
  "opprettetAv": "mockSaksbehandler"
}"""
        justRun { vedtakService.validerKanLagreVedtak(any()) }
        justRun { vedtakService.slettVedtakHvisFinnes(any()) }

        every { vedtakService.lagreVedtak(any(), any()) } returns UUID.fromString("86460749-53d4-481b-8909-7286efc7eaad")

        val responseJson =
            mockMvc
                .post("/api/vedtak/86460749-53d4-481b-8909-7286efc7eaad/lagre-vedtak") {
                    contentType = MediaType.APPLICATION_JSON
                    content = json
                }.andExpect {
                    status { isOk() }
                }.andReturn()
                .response.contentAsString

        verify(exactly = 1) {
            vedtakService.lagreVedtak(any(), any())
        }
    }
}
