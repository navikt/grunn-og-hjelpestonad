package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.InfotrygdEvaluering

/** Løpende grunnstønad der siste vedtak er et førstegangsvedtak med ønsket sats. */
data class Migreringskandidat(
    val personident: String,
    val stønadId: Long,
    val vedtakId: Long,
)

/**
 * Går gjennom kandidatene parti for parti, sortert på stønadId synkende, og samler personer der siste
 * evaluering har [hjemmelbokstav]. Stopper når [antall] personer er funnet, eller når et parti er tomt.
 *
 * Hjemmelbokstaven sammenlignes på trimmet tekst. En evaluering med manglende eller motstridende
 * hjemmelbokstav har `hjemmelbokstav = null` og gir ikke treff.
 *
 * [hentParti] får stønadId-en partiet skal starte under, eller null for første parti.
 */
fun finnPersonerMedHjemmelbokstav(
    hjemmelbokstav: String,
    antall: Int,
    hentParti: (førStønadId: Long?) -> List<Migreringskandidat>,
    hentEvalueringer: (vedtakIder: Set<Long>) -> Map<Long, InfotrygdEvaluering>,
): Set<String> {
    val ønsketHjemmelbokstav = hjemmelbokstav.trim()
    val personerMedTreff = linkedSetOf<String>()
    var førStønadId: Long? = null

    while (personerMedTreff.size < antall) {
        val kandidaterIPartiet = hentParti(førStønadId)
        if (kandidaterIPartiet.isEmpty()) break

        val evalueringerPerVedtak = hentEvalueringer(kandidaterIPartiet.map { it.vedtakId }.toSet())
        kandidaterIPartiet
            .filter { kandidat -> evalueringerPerVedtak[kandidat.vedtakId]?.hjemmelbokstav?.trim() == ønsketHjemmelbokstav }
            .forEach { kandidat -> if (personerMedTreff.size < antall) personerMedTreff.add(kandidat.personident) }

        val lavesteStønadIdIPartiet = kandidaterIPartiet.minOf { it.stønadId }
        check(førStønadId == null || lavesteStønadIdIPartiet < førStønadId) {
            "Partiet må ha stønadId under $førStønadId, ellers går løkka evig"
        }
        førStønadId = lavesteStønadIdIPartiet
    }
    return personerMedTreff
}
