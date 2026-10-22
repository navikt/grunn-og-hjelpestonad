package no.nav.grunn.og.hjelpestonad.config

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestClient

/**
 * springdoc navngir skjemaer etter klassenavnet uten pakke. To klasser med samme navn gir ett skjema, og det ene
 * overskriver det andre uten feil. Modellene som genereres fra infotrygd-kontrakten og sendes videre til frontend,
 * har derfor prefikset Infotrygd (modelNameMappings i pom.xml). Testen feiler hvis de kolliderer igjen.
 */
class OpenApiSkjemanavnTest(
    @param:Value("\${local.server.port}") private val port: Int,
) : SpringContextTest() {
    @Test
    fun `vedtak i sak og vedtak fra Infotrygd er egne skjemaer`() {
        val skjemaer = hentSkjemaer()

        assertThat(egenskaper(skjemaer, "VedtakResponse")).contains("resultatType")
        assertThat(egenskaper(skjemaer, "InfotrygdVedtakResponse")).contains("tknr", "delytelser")
        assertThat(egenskaper(skjemaer, "InfotrygdStonadResponse")).contains("stonadId", "vedtak")
    }

    @Suppress("UNCHECKED_CAST")
    private fun hentSkjemaer(): Map<String, Any?> {
        val apiDocs =
            RestClient
                .create("http://localhost:$port")
                .get()
                .uri("/v3/api-docs")
                .retrieve()
                .body(Map::class.java) as Map<String, Any?>
        val components = apiDocs["components"] as Map<String, Any?>
        return components["schemas"] as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun egenskaper(
        skjemaer: Map<String, Any?>,
        navn: String,
    ): Set<String> {
        val skjema = skjemaer[navn] as? Map<String, Any?> ?: error("Fant ikke skjemaet $navn")
        return (skjema["properties"] as Map<String, Any?>).keys
    }
}
