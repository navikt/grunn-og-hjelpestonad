package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PeriodeResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.PerioderResponse
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Icd10

/** /perioder fra infotrygd, med ICD-10-tekst for diagnosene i evalueringene. Inneholder helseopplysninger (GDPR art. 9). */
data class InfotrygdPerioderResponse(
    val grunnstønad: List<PeriodeResponse>,
    val hjelpestønad: List<PeriodeResponse>,
    /** ICD-10-tekst per diagnosekode (diagnose1 og diagnose2, trimmet). Koder uten treff i ICD-10 er ikke med. */
    val diagnosetekster: Map<String, String>,
)

fun PerioderResponse.tilInfotrygdPerioderResponse(): InfotrygdPerioderResponse {
    val diagnosekoder =
        (grunnstønad + hjelpestønad)
            .flatMap { periode -> listOfNotNull(periode.evaluering?.diagnose1, periode.evaluering?.diagnose2) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    return InfotrygdPerioderResponse(
        grunnstønad = grunnstønad,
        hjelpestønad = hjelpestønad,
        diagnosetekster =
            diagnosekoder
                .mapNotNull { kode -> Icd10.finn(kode)?.tekst?.let { tekst -> kode to tekst } }
                .toMap(),
    )
}
