package no.nav.grunn.og.hjelpestonad.prosessering

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.type.filter.AssignableTypeFilter

class ProsesseringProfilTest {
    @ParameterizedTest
    @ValueSource(strings = ["prod", "local-mock", "integrasjonstest", "dev,prod", "dev,local-mock", "dev"])
    fun `controller sikkerhetskjede og tilgangskontroll finnes bare i dev`(profiler: String) {
        val miljo = StandardEnvironment().apply { setActiveProfiles(*profiler.split(",").toTypedArray()) }
        val skanner = ClassPathScanningCandidateComponentProvider(false, miljo)
        skanner.addIncludeFilter(AssignableTypeFilter(ProsesseringController::class.java))
        skanner.addIncludeFilter(AssignableTypeFilter(ProsesseringSecurityConfig::class.java))
        skanner.addIncludeFilter(AssignableTypeFilter(ProsesseringTilgang::class.java))
        val komponenter = skanner.findCandidateComponents("no.nav.grunn.og.hjelpestonad.prosessering")
        assertThat(komponenter).hasSize(if (profiler == "dev") 3 else 0)
    }
}
