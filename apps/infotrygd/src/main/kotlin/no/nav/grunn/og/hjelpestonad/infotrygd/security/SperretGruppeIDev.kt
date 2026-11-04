package no.nav.grunn.og.hjelpestonad.infotrygd.security

/**
 * Grupper som gir tilgang til personer med adressebeskyttelse eller skjerming. I dev kan alle gi seg selv disse
 * gruppene, og Infotrygd i dev har skarpe data. En saksbehandler med én av gruppene får derfor ikke slå opp
 * personer i dev. Se ADR-0015.
 *
 * ID-ene finnes bare i dev-tenanten (`trygdeetaten.no`) og er de samme som tilgangsmaskinen bruker i dev-gcp.
 * Strengt fortrolig adresse dekker også strengt fortrolig utland (§ 19). Gruppene må stå i `claims.groups` i
 * `.nais/dev.yaml`, ellers kommer de ikke med i tokenet.
 */
enum class SperretGruppeIDev(
    val gruppeId: String,
    val beskrivelse: String,
) {
    STRENGT_FORTROLIG_ADRESSE("a1b518e2-3947-478d-8929-e5d685c47cac", "strengt fortrolig adresse"),
    FORTROLIG_ADRESSE("2e1dc582-f762-4510-a660-88bf68fb7128", "fortrolig adresse"),
    EGNE_ANSATTE("dbe4ad45-320b-4e9a-aaa1-73cca4ee124d", "skjerming"),
    ;

}
