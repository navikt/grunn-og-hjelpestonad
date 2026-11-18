package no.nav.grunn.og.hjelpestonad.behandling

/**
 * Stegene i en behandling. [rekkefølge] bestemmer hvilke steg som kommer før andre, og brukes når et steg
 * sammenlignes med nåværende steg. Rekkefølgen i koden under har ingen betydning.
 */
enum class BehandlingSteg(
    val rekkefølge: Int,
    val visningsnavn: String,
) {
    ÅRSAK(rekkefølge = 1, visningsnavn = "Årsak behandling"),
    INNGANGSVILKÅR(rekkefølge = 2, visningsnavn = "Vilkår"),
    SATS_OG_TRYGDETID(rekkefølge = 3, visningsnavn = "Sats og trygdetid"),
    VEDTAK_OG_BREV(rekkefølge = 4, visningsnavn = "Simulering og brev"),
}
