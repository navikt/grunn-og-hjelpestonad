package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

/**
 * Valideringen er ikke komplett. Bærer høyst én feil per nøkkel og beholder alle meldingene.
 * Håndteres av en lokal `@ExceptionHandler` i
 * `InngangsvilkårStegController` og gir HTTP 400.
 */
class VilkårValideringFeil(
    feil: List<Valideringsfeil>,
) : RuntimeException("Valideringen er ikke komplett") {
    val feil: List<Valideringsfeil> =
        feil
            .groupBy { it.nøkkel }
            .map { (nøkkel, feilMedSammeNøkkel) ->
                Valideringsfeil(
                    nøkkel = nøkkel,
                    melding = feilMedSammeNøkkel.joinToString(" ") { it.melding },
                )
            }
}
