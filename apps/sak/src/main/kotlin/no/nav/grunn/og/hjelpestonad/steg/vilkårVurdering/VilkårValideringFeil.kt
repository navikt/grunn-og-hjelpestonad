package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

/**
 * Vilkårsvurderingen er ikke komplett. Bærer én feil per vilkår slik at frontend kan lenke
 * saksbehandler til riktig vilkår. Håndteres av en lokal `@ExceptionHandler` i
 * `VilkårVurderingStegController` og gir HTTP 400.
 */
class VilkårValideringFeil(
    val feil: List<Valideringsfeil>,
) : RuntimeException("Vilkårsvurderingen er ikke komplett")
