package no.nav.grunn.og.hjelpestonad.infotrygd.stønad

import no.nav.grunn.og.hjelpestonad.infotrygd.evaluering.InfotrygdEvaluering

/** Løpende grunnstønad der siste vedtak er et førstegangsvedtak med ønsket sats. */
data class Migreringskandidat(
    val personident: String,
    val stønadId: Long,
    val vedtakId: Long,
)

/**
 * Går gjennom [kandidater] i rekkefølge, i biter på [bitstørrelse], og samler personer der siste evaluering har
 * [hjemmelbokstav]. Evalueringene hentes én bit om gangen, og gjennomgangen stopper når [antall] personer er funnet.
 *
 * Hjemmelbokstaven sammenlignes på trimmet tekst. En evaluering med manglende eller motstridende hjemmelbokstav har
 * `hjemmelbokstav = null` og gir ikke treff.
 */
fun finnPersonerMedHjemmelbokstav(
    kandidater: List<Migreringskandidat>,
    hjemmelbokstav: String,
    antall: Int,
    bitstørrelse: Int,
    hentEvalueringer: (vedtakIder: Set<Long>) -> Map<Long, InfotrygdEvaluering>,
): Set<String> {
    require(bitstørrelse > 0) { "bitstørrelse må være større enn 0" }
    val ønsketHjemmelbokstav = hjemmelbokstav.trim()
    val personerMedTreff = linkedSetOf<String>()

    for (kandidaterIBiten in kandidater.chunked(bitstørrelse)) {
        if (personerMedTreff.size >= antall) break
        val evalueringerPerVedtak = hentEvalueringer(kandidaterIBiten.map { it.vedtakId }.toSet())
        kandidaterIBiten
            .filter { kandidat -> evalueringerPerVedtak[kandidat.vedtakId]?.hjemmelbokstav?.trim() == ønsketHjemmelbokstav }
            .forEach { kandidat -> if (personerMedTreff.size < antall) personerMedTreff.add(kandidat.personident) }
    }
    return personerMedTreff
}
