package no.nav.grunn.og.hjelpestonad.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Springdoc beskriver et nullbart Kotlin-enum som `type: ["string", "null"]` med en enum-liste
 * som *ikke* inneholder `null`. Etter JSON Schema vinner enum-listen, så dokumentet sier i
 * praksis at `null` er ugyldig. Kodegeneratorer (f.eks. hey-api/zod i frontend) tar det på
 * ordet og lager en valideringsregel som avviser `null` — selv om backend faktisk sender det.
 *
 * Denne customizeren legger `null` inn i enum-listen for alle skjemaer som er markert nullbare,
 * slik at dokumentet stemmer med det API-et returnerer.
 *
 * Det er verdt å sjekke om dette blir fikset med https://github.com/swagger-api/swagger-core/issues/4991
 */
@Configuration
open class OpenApiNullbarEnumConfig {
    @Bean
    open fun nullbarEnumCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi -> normaliser(openApi) }

    private fun normaliser(openApi: OpenAPI) {
        val besøkte = Collections.newSetFromMap(IdentityHashMap<Schema<*>, Boolean>())

        openApi.components
            ?.schemas
            ?.values
            ?.forEach { besøk(it, besøkte) }
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations().forEach { operasjon ->
                operasjon.parameters?.forEach { besøk(it.schema, besøkte) }
                operasjon.requestBody
                    ?.content
                    ?.values
                    ?.forEach { besøk(it.schema, besøkte) }
                operasjon.responses?.values?.forEach { respons ->
                    respons.content?.values?.forEach { besøk(it.schema, besøkte) }
                }
            }
        }
    }

    private fun besøk(
        schema: Schema<*>?,
        besøkte: MutableSet<Schema<*>>,
    ) {
        if (schema == null || !besøkte.add(schema)) return

        if (erNullbar(schema) && !schema.enum.isNullOrEmpty() && schema.enum.none { it == null }) {
            @Suppress("UNCHECKED_CAST")
            (schema as Schema<Any?>).enum = schema.enum + null
        }

        schema.properties?.values?.forEach { besøk(it, besøkte) }
        besøk(schema.items, besøkte)
        besøk(schema.not, besøkte)
        (schema.additionalProperties as? Schema<*>)?.let { besøk(it, besøkte) }
        listOfNotNull(schema.allOf, schema.anyOf, schema.oneOf)
            .flatten()
            .forEach { besøk(it, besøkte) }
    }

    /** OpenAPI 3.1 bruker `types`-settet, 3.0 bruker `nullable`-flagget. */
    private fun erNullbar(schema: Schema<*>): Boolean = schema.types?.contains("null") == true || schema.nullable == true
}
