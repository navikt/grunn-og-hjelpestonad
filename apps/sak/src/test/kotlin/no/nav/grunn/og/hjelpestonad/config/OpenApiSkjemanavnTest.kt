package no.nav.grunn.og.hjelpestonad.config

import no.nav.grunn.og.hjelpestonad.SpringContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestClient

/**
 * springdoc navngir skjemaer etter klassenavnet uten pakke. To klasser med samme navn gir ett skjema, og det ene
 * overskriver det andre uten feil. Typene fra Infotrygd som sendes til frontend har derfor egne navn, og
 * modelNameMappings i pom.xml gir den genererte kontrakten Infotrygd-prefiks. Testen feiler hvis de kolliderer igjen.
 */
class OpenApiSkjemanavnTest(
    @param:Value("\${local.server.port}") private val port: Int,
) : SpringContextTest() {
    @Test
    fun `vedtak i sak og vedtak fra Infotrygd er egne skjemaer`() {
        val skjemaer = hentSkjemaer()

        assertThat(egenskaper(skjemaer, "VedtakResponse")).contains("resultatType")
        assertThat(egenskaper(skjemaer, "InfotrygdVedtakMedArv")).contains("tknr", "delytelser", "evaluering", "gh")
        assertThat(egenskaper(skjemaer, "VerdiMedKildeInfotrygdEvaluering")).contains("verdi", "arvetFraVedtak")
        assertThat(egenskaper(skjemaer, "VerdiMedKildeListInfotrygdGhResponse")).contains("verdi", "arvetFraVedtak")
        assertThat(egenskaper(skjemaer, "InfotrygdStonadMedArv")).contains("stonadId", "vedtak")
        assertThat(egenskaper(skjemaer, "GjeldendeInfotrygdPeriode")).contains("fom", "tom", "stonadId", "vedtak")
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
