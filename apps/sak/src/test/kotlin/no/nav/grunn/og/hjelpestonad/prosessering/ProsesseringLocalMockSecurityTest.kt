package no.nav.grunn.og.hjelpestonad.prosessering

import io.mockk.every
import no.nav.grunn.og.hjelpestonad.config.MockSecurityConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.YamlMapFactoryBean
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

@ActiveProfiles(profiles = ["local-mock"], inheritProfiles = false)
@Import(MockSecurityConfig::class)
class ProsesseringLocalMockSecurityTest : ProsesseringSecurityTest() {
    @Autowired
    private lateinit var localMockJwtDecoder: JwtDecoder

    @Autowired
    private lateinit var localMockProsesseringService: ProsesseringService

    @Autowired
    private lateinit var localMockTilgang: ProsesseringTilgang

    @Test
    fun `mock-token fra docker compose oppfyller lokal tilgangskonfigurasjon`() {
        val composeKonfigurasjon =
            YamlMapFactoryBean()
                .apply {
                    setResources(FileSystemResource("docker-compose.yml"))
                }.`object`!!
        val objectMapper = jacksonObjectMapper()
        val composeJson: JsonNode = objectMapper.valueToTree(composeKonfigurasjon)
        val oauthKonfigurasjon =
            objectMapper.readTree(
                composeJson["services"]["mock-oauth2-server"]["environment"]["JSON_CONFIG"].asText(),
            )
        val claims = oauthKonfigurasjon["tokenCallbacks"][0]["requestMappings"][0]["claims"]
        val mockBrukertoken =
            Jwt
                .withTokenValue("lokalt-brukertoken")
                .header("alg", "RS256")
                .claims { it.putAll(objectMapper.convertValue(claims, object : TypeReference<Map<String, Any>>() {})) }
                .build()
        val authentication = JwtAuthenticationToken(mockBrukertoken, emptyList())
        assertThat(localMockTilgang.harRolleProsessering(authentication)).isEqualTo("A123456")
        every { localMockJwtDecoder.decode("lokalt-brukertoken") } returns mockBrukertoken
        every { localMockProsesseringService.hentStatusoversikt() } returns emptyList()

        mockMvc
            .get("/api/prosessering/task/status-oversikt") { header("Authorization", "Bearer lokalt-brukertoken") }
            .andExpect { status { isOk() } }
    }
}
