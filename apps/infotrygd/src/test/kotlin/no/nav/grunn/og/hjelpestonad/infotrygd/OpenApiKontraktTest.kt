package no.nav.grunn.og.hjelpestonad.infotrygd

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Skriver OpenAPI-spesifikasjonen til `openapi.json`. Sak genererer klientmodellene sine fra denne filen, så
 * den er kontrakten mellom appene. Testen feiler når filen var utdatert, slik at endringen blir committet.
 */
@SpringBootTest(classes = [Application::class])
@ActiveProfiles("test")
class OpenApiKontraktTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Test
    fun `openapi-json er oppdatert`() {
        val spec =
            MockMvcBuilders
                .webAppContextSetup(context)
                .build()
                .perform(get("/v3/api-docs"))
                .andReturn()
                .response
                .contentAsString
        val mapper = JsonMapper.builder().build()
        val formatert = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(spec)) + "\n"

        val fil = Path.of("openapi.json")
        val eksisterende = if (Files.exists(fil)) Files.readString(fil) else null
        if (eksisterende != formatert) {
            Files.writeString(fil, formatert)
        }

        assertTrue(eksisterende == formatert, "openapi.json var utdatert og er nå oppdatert. Commit endringen.")
    }
}
