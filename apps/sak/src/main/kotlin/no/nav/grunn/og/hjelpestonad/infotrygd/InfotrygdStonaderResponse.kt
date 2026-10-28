package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Icd10

/** /stonader fra infotrygd, med ICD-10-tekst for diagnosene i evalueringene. Inneholder helseopplysninger (GDPR art. 9). */
data class InfotrygdStonaderResponse(
    val stonader: List<InfotrygdStonadMedArv>,
    /** ICD-10-tekst per diagnosekode (diagnose1 og diagnose2, trimmet). Koder uten treff i ICD-10 er ikke med. */
    val diagnosetekster: Map<String, String>,
)

fun List<InfotrygdStonadMedArv>.tilInfotrygdStonaderResponse(): InfotrygdStonaderResponse =
    InfotrygdStonaderResponse(
        stonader = this,
        diagnosetekster = diagnosetekster(flatMap { it.vedtak }),
    )

/** ICD-10-tekst for diagnosene i [vedtak]. Brukes av både stønads- og gjeldende-periode-svaret. */
internal fun diagnosetekster(vedtak: List<InfotrygdVedtakMedArv>): Map<String, String> {
    val diagnosekoder =
        vedtak
            .flatMap { listOfNotNull(it.evaluering?.verdi?.diagnose1, it.evaluering?.verdi?.diagnose2) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    return diagnosekoder
        .mapNotNull { kode -> Icd10.finn(kode)?.tekst?.let { tekst -> kode to tekst } }
        .toMap()
}
