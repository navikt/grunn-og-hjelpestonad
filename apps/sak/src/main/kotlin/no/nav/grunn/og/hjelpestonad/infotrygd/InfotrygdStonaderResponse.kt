package no.nav.grunn.og.hjelpestonad.infotrygd

import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.InfotrygdStonadResponse
import no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt.StonaderResponse
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.Icd10

/** /stonader fra infotrygd, med ICD-10-tekst for diagnosene i evalueringene. Inneholder helseopplysninger (GDPR art. 9). */
data class InfotrygdStonaderResponse(
    val stonader: List<InfotrygdStonadResponse>,
    /** ICD-10-tekst per diagnosekode (diagnose1 og diagnose2, trimmet). Koder uten treff i ICD-10 er ikke med. */
    val diagnosetekster: Map<String, String>,
)

fun StonaderResponse.tilInfotrygdStonaderResponse(): InfotrygdStonaderResponse {
    val diagnosekoder =
        stonader
            .flatMap { it.vedtak }
            .flatMap { vedtak -> listOfNotNull(vedtak.evaluering?.diagnose1, vedtak.evaluering?.diagnose2) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    return InfotrygdStonaderResponse(
        stonader = stonader,
        diagnosetekster =
            diagnosekoder
                .mapNotNull { kode -> Icd10.finn(kode)?.tekst?.let { tekst -> kode to tekst } }
                .toMap(),
    )
}
