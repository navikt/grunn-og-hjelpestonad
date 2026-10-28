package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdVedtakResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Feltene i den genererte Infotrygd-kontrakten må finnes med samme navn i [InfotrygdVedtakMedArv] og
 * [InfotrygdStonadMedArv]. Når kontrakten får et nytt felt, feiler testen til feltet er mappet i InfotrygdArv.kt.
 */
class InfotrygdKontraktTest {
    @Test
    fun `InfotrygdVedtakMedArv har alle feltene i kontraktens vedtak`() {
        assertThat(egenskaper(InfotrygdVedtakMedArv::class)).containsAll(egenskaper(InfotrygdVedtakResponse::class))
    }

    @Test
    fun `InfotrygdStonadMedArv har alle feltene i kontraktens stønad`() {
        assertThat(egenskaper(InfotrygdStonadMedArv::class)).containsAll(egenskaper(InfotrygdStonadResponse::class))
    }

    private fun egenskaper(klasse: KClass<*>): Set<String> = klasse.memberProperties.map { it.name }.toSet()
}
