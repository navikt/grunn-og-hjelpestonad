package no.nav.grunn.og.hjelpestonad.prosessering

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringJUnitWebConfig(ProsesseringSecurityTest.TestConfig::class)
@ActiveProfiles("dev")
class ProsesseringSecurityTest {
    @TestConfiguration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(
        ProsesseringController::class,
        ProsesseringSecurityConfig::class,
        ProsesseringTilgang::class,
        ProsesseringExceptionHandler::class,
    )
    open class TestConfig {
        @Bean
        open fun prosesseringService(): ProsesseringService = mockk()

        @Bean
        open fun jwtDecoder(): JwtDecoder = mockk()
    }

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var prosesseringService: ProsesseringService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun settOpp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @Test
    fun `alle endepunkter er stengt av stubben og kaller ikke servicen`() {
        val endepunkter =
            listOf(
                HttpMethod.GET to "",
                HttpMethod.GET to "/status-oversikt",
                HttpMethod.GET to "/typer",
                HttpMethod.GET to "/42",
                HttpMethod.GET to "/42/logg",
                HttpMethod.POST to "/42/rekjor",
                HttpMethod.POST to "/42/avbryt",
                HttpMethod.POST to "/42/avvik",
            )
        for (endepunkt in endepunkter) {
            mockMvc
                .perform(
                    request(endepunkt.first, "/api/prosessering/task${endepunkt.second}")
                        .with(
                            jwt().jwt {
                                it.claim("NAVident", "Z123456")
                                it.claim("groups", listOf("928636f4-fd0d-4149-978e-a6fb68bb19de"))
                            },
                        ).contentType(MediaType.APPLICATION_JSON)
                        .content("""{"versjon":7,"melding":"Undersøkt manuelt"}"""),
                ).andExpect(status().isForbidden)
        }
        verify { prosesseringService wasNot Called }
    }

    @Test
    fun `manglende token gir 401`() {
        mockMvc.get("/api/prosessering/task/status-oversikt").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `ugyldig token gir 401`() {
        every { jwtDecoder.decode("ugyldig") } throws BadJwtException("Ugyldig token")
        mockMvc
            .get("/api/prosessering/task/status-oversikt") { header("Authorization", "Bearer ugyldig") }
            .andExpect { status { isUnauthorized() } }
    }
}
