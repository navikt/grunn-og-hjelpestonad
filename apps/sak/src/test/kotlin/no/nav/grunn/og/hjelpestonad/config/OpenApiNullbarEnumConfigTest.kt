package no.nav.grunn.og.hjelpestonad.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class OpenApiNullbarEnumConfigTest {
    private val customizer = OpenApiNullbarEnumConfig().nullbarEnumCustomizer()

    private fun nullbartEnum(): Schema<String> =
        StringSchema().apply {
            types = setOf("string", "null")
            addEnumItemObject("JA")
            addEnumItemObject("NEI")
        }

    private fun openApiMed(schema: Schema<*>): OpenAPI = OpenAPI().components(Components().addSchemas("Test", ObjectSchema().addProperty("felt", schema)))

    @Test
    fun `legger null i enum-listen for nullbare enums`() {
        val openApi = openApiMed(nullbartEnum())

        customizer.customise(openApi)

        val felt = openApi.components.schemas["Test"]!!.properties["felt"]!!
        assertThat(felt.enum).containsExactly("JA", "NEI", null)
    }

    @Test
    fun `lar enums som ikke er nullbare være i fred`() {
        val openApi = openApiMed(StringSchema().apply { addEnumItemObject("JA") })

        customizer.customise(openApi)

        val felt = openApi.components.schemas["Test"]!!.properties["felt"]!!
        assertThat(felt.enum).containsExactly("JA")
    }

    @Test
    fun `legger ikke null inn to ganger`() {
        val openApi = openApiMed(nullbartEnum())

        customizer.customise(openApi)
        customizer.customise(openApi)

        val felt = openApi.components.schemas["Test"]!!.properties["felt"]!!
        assertThat(felt.enum).containsExactly("JA", "NEI", null)
    }

    @Test
    fun `finner nullbare enums inne i lister`() {
        val openApi =
            OpenAPI().components(
                Components().addSchemas(
                    "Test",
                    io.swagger.v3.oas.models.media
                        .ArraySchema()
                        .items(nullbartEnum()),
                ),
            )

        customizer.customise(openApi)

        assertThat(
            openApi.components.schemas["Test"]!!
                .items.enum,
        ).containsExactly("JA", "NEI", null)
    }
}
