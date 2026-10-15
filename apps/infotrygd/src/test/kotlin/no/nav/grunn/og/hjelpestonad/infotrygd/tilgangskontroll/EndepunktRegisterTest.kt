package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Sikrer at hvert REST-endepunkt er plassert: enten sjekker det tilgang til personer via TilgangService,
 * eller det er bevisst unntatt. Et nytt endepunkt får testen til å feile til det er klassifisert.
 * Se ADR-0010.
 */
class EndepunktRegisterTest {
    @Test
    fun `alle endepunkter er klassifisert`() {
        val funnet = alleHandlere()

        val ikkeKlassifisert = funnet - (MED_PERSONSJEKK + UTEN_PERSONSJEKK)
        val finnesIkke = (MED_PERSONSJEKK + UTEN_PERSONSJEKK) - funnet

        assertTrue(
            ikkeKlassifisert.isEmpty(),
            "Nye endepunkter må kalle tilgangService og legges i MED_PERSONSJEKK, " +
                "eller legges i UTEN_PERSONSJEKK med begrunnelse (se ADR-0010): $ikkeKlassifisert",
        )
        assertTrue(finnesIkke.isEmpty(), "Endepunkter i listene som ikke finnes lenger: $finnesIkke")
    }

    @Test
    fun `ingen endepunkter står i begge listene`() {
        assertEquals(emptySet<String>(), MED_PERSONSJEKK intersect UTEN_PERSONSJEKK)
    }

    private fun alleHandlere(): Set<String> {
        val miljø = StandardEnvironment().apply { setActiveProfiles("dev") }
        val skanner = ClassPathScanningCandidateComponentProvider(false, miljø)
        skanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))

        return skanner
            .findCandidateComponents("no.nav.grunn.og.hjelpestonad.infotrygd")
            .map { Class.forName(it.beanClassName) }
            .flatMap { klasse ->
                klasse.declaredMethods
                    .filter { AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java) }
                    .map { "${klasse.simpleName}#${it.name}" }
            }.toSet()
    }

    companion object {
        private val MED_PERSONSJEKK =
            setOf(
                "DiagnoseController#hentDiagnoser",
                "PeriodeController#hentPerioder",
                "SakController#finnSaker",
                "StønadController#eksisterer",
                "StønadController#hentPersonerForMigrering",
            )

        // Returnerer bare metadata om tabellene, ingen persondata.
        private val UTEN_PERSONSJEKK =
            setOf(
                "TabellController#hentKolonnerPerTabell",
                "TabellController#hentAntallRaderPerTabell",
            )
    }
}
