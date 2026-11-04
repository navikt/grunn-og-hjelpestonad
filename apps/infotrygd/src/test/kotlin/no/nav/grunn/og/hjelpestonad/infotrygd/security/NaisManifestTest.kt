package no.nav.grunn.og.hjelpestonad.infotrygd.security

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Sjekker at Nais-manifestene matcher sperren i [SperretGruppeIDev]. Gruppene må stå i `claims.groups` i dev,
 * ellers kommer de ikke med i tokenet og sperren virker ikke.
 */
class NaisManifestTest {
    @Test
    fun `alle sperrede grupper står i claims groups i dev-manifestet`() {
        val manifest = lesManifest(DEV_MANIFEST)
        val gruppeIder = claimsGroupIder(manifest)

        val manglende = SperretGruppeIDev.entries.filter { it.gruppeId !in gruppeIder }

        assertTrue(
            manglende.isEmpty(),
            "Mangler i spec.azure.application.claims.groups i .nais/dev.yaml: " +
                manglende.joinToString { "${it.name} (${it.gruppeId})" },
        )
    }

    @Test
    fun `infotrygd har ikke prod-manifest uten at sperren er vurdert`() {
        val prodManifester = PROD_MANIFESTER.map(Path::of).filter(Files::exists)

        assertTrue(
            prodManifester.isEmpty(),
            "Sperren i SperretGruppeIDev bruker dev-gruppe-ID-er. Ta stilling til den (ADR-0015) før infotrygd får prod-manifest, og oppdater så denne testen.",
        )
    }

    private fun lesManifest(sti: String): Map<*, *> {
        val path = Path.of(sti)
        assertTrue(
            Files.exists(path),
            "Fant ikke ${path.toAbsolutePath()}. Testen må kjøres med apps/infotrygd som working dir.",
        )
        return Yaml().load<Map<*, *>>(Files.readString(path))
    }

    private fun claimsGroupIder(manifest: Map<*, *>): Set<String> {
        val groups =
            manifest
                .hent("spec")
                .hent("azure")
                .hent("application")
                .hent("claims")["groups"] as? List<*>
                ?: error("Mangler spec.azure.application.claims.groups i .nais/dev.yaml")
        return groups.map { gruppe -> (gruppe as Map<*, *>)["id"] as String }.toSet()
    }

    private fun Map<*, *>.hent(nøkkel: String): Map<*, *> = this[nøkkel] as? Map<*, *> ?: error("Mangler '$nøkkel' i .nais/dev.yaml")

    private companion object {
        const val DEV_MANIFEST = ".nais/dev.yaml"
        val PROD_MANIFESTER = listOf(".nais/prod.yaml", ".nais/prod-gcp.yaml")
    }
}
