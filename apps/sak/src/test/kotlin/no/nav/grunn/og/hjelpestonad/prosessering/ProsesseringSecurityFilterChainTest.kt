package no.nav.grunn.og.hjelpestonad.prosessering

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringJUnitWebConfig(ProsesseringSecurityFilterChainTest.TestConfig::class)
class ProsesseringSecurityFilterChainTest {
    @TestConfiguration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(ProsesseringSecurityConfig::class, ProsesseringTilgang::class, TestController::class)
    open class TestConfig {
        @Bean
        open fun jwtDecoder(): JwtDecoder = JwtDecoder { throw BadJwtException("Ugyldig token") }
    }

    @RestController
    class TestController {
        @GetMapping("/api/prosessering/test")
        fun hentTestverdi(): String = "ok"
    }

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

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
    fun `delegert brukertoken med prosesseringsrollen slipper gjennom filteret`() {
        mockMvc
            .get("/api/prosessering/test") { with(brukertoken()) }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `bruker uten prosesseringsrollen avvises selv uten tilgangssjekk i controlleren`() {
        mockMvc
            .get("/api/prosessering/test") { with(brukertoken(grupper = listOf("annen-gruppe"))) }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `maskintoken med prosesseringsgruppen avvises i filteret`() {
        mockMvc
            .get("/api/prosessering/test") {
                with(brukertoken(identitetstype = "app"))
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `brukertoken uten NAVident avvises i filteret`() {
        mockMvc
            .get("/api/prosessering/test") {
                with(brukertoken(navIdent = null))
            }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `manglende token gir 401 fra filteret`() {
        mockMvc
            .get("/api/prosessering/test")
            .andExpect { status { isUnauthorized() } }
    }

    private fun brukertoken(
        grupper: List<String> = listOf("bbda72af-39ba-48bc-992b-341161bf21f1"),
        navIdent: String? = "Z123456",
        identitetstype: String = "user",
    ): JwtRequestPostProcessor =
        jwt().jwt {
            it.subject("bruker-subjekt")
            it.claim("oid", "bruker-objekt")
            it.claim("idtyp", identitetstype)
            it.claim("scp", "access_as_user")
            if (navIdent != null) it.claim("NAVident", navIdent)
            it.claim("groups", grupper)
        }
}
