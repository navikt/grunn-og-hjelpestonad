package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProsesseringControllerTest {
    private val prosesseringService = mockk<ProsesseringService>()
    private val prosesseringTilgang = mockk<ProsesseringTilgang>()
    private val authentication = UsernamePasswordAuthenticationToken("Z123456", "ikke-brukt")
    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(ProsesseringController(prosesseringService, prosesseringTilgang))
            .setControllerAdvice(ProsesseringExceptionHandler())
            .build()

    init {
        every { prosesseringTilgang.krevTilgang(authentication) } returns "Z123456"
    }

    @Test
    fun `task serialiseres som kontrakten uten mellomlagring`() {
        every { prosesseringService.hentTask(42, "Z123456") } returns prosesseringTask()
        mockMvc
            .get("/api/prosessering/task/42") { principal = authentication }
            .andExpect {
                status { isOk() }
                header { string("Cache-Control", "no-store") }
                jsonPath("$.id") { value(42) }
                jsonPath("$.status") { value("STOPPET") }
                jsonPath("$.metadata.traceId") { value("test-trace") }
                jsonPath("$.payload") { value("""{"sensitiv":"testverdi"}""") }
                jsonPath("$.versjon") { value(7) }
                jsonPath("$.sistEndretTid") { value("2026-11-18T12:00:00Z") }
            }
    }

    @Test
    fun `tasklisten har enindekserte standardverdier`() {
        every { prosesseringService.hentTasks(null, null, 1, 25, "Z123456") } returns
            ProsesseringTaskSide(listOf(prosesseringTask()), 1, 25, 1, 1)
        mockMvc
            .get("/api/prosessering/task") { principal = authentication }
            .andExpect {
                status { isOk() }
                jsonPath("$.side") { value(1) }
                jsonPath("$.antallPerSide") { value(25) }
                jsonPath("$.antallTotalt") { value(1) }
                jsonPath("$.avkortet") { value(false) }
            }
    }

    @Test
    fun `statusoversikt returnerer alle fem statuser`() {
        every { prosesseringService.hentStatusoversikt() } returns Status.entries.map { ProsesseringStatusTelling(it, 0) }
        mockMvc
            .get("/api/prosessering/task/status-oversikt") { principal = authentication }
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(5) }
            }
        verify { prosesseringTilgang.krevTilgang(authentication) }
    }

    @Test
    fun `ugyldig filter returnerer problem uten input i feilsvaret`() {
        mockMvc
            .get("/api/prosessering/task?status=sensitiv-testverdi") { principal = authentication }
            .andExpect {
                status { isBadRequest() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                jsonPath("$.detail") { value("Ugyldig forespørsel. Kontroller filter, paginering og påkrevde felt.") }
            }
    }

    @Test
    fun `rekjor returnerer oppdatert task`() {
        every { prosesseringService.endreTask(42, 7, ProsesseringHandling.REKJOR, "Z123456") } returns
            prosesseringTask(Status.KLAR).copy(versjon = 8)
        mockMvc
            .post("/api/prosessering/task/42/rekjor") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"versjon":7}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.versjon") { value(8) }
                jsonPath("$.status") { value("KLAR") }
            }
    }

    @Test
    fun `manglende versjon avvises`() {
        mockMvc
            .post("/api/prosessering/task/42/rekjor") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }.andExpect { status { isBadRequest() } }
        verify(exactly = 0) { prosesseringService.endreTask(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `null versjon avvises`() {
        mockMvc
            .post("/api/prosessering/task/42/rekjor") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"versjon":null}"""
            }.andExpect { status { isBadRequest() } }
        verify(exactly = 0) { prosesseringService.endreTask(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `avvik tar ident fra autentisering og ikke request body`() {
        every {
            prosesseringService.endreTask(42, 7, ProsesseringHandling.AVVIK, "Z123456", "Undersøkt manuelt")
        } returns prosesseringTask(Status.AVBRUTT).copy(versjon = 8)
        mockMvc
            .post("/api/prosessering/task/42/avvik") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"versjon":7,"melding":"Undersøkt manuelt"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.status") { value("AVBRUTT") }
            }
    }

    @Test
    fun `konflikt returneres som problem med detail`() {
        every { prosesseringService.endreTask(42, 7, ProsesseringHandling.AVBRYT, "Z123456") } throws
            ProsesseringApiException(org.springframework.http.HttpStatus.CONFLICT, "Noen andre har endret tasken.")
        mockMvc
            .post("/api/prosessering/task/42/avbryt") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"versjon":7}"""
            }.andExpect {
                status { isConflict() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                jsonPath("$.detail") { value("Noen andre har endret tasken.") }
            }
    }
}
