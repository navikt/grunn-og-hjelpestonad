package no.nav.grunn.og.hjelpestonad.util

import java.util.Locale
import java.util.MissingResourceException

private val bokmål = Locale.of("nb", "NO")

private val alpha3TilAlpha2: Map<String, String> =
    Locale
        .getISOCountries()
        .mapNotNull { alpha2 ->
            try {
                Locale.of("", alpha2).isO3Country to alpha2
            } catch (_: MissingResourceException) {
                null
            }
        }.toMap()

/**
 * Landnavn på bokmål for en landkode i ISO 3166-1 alfa-3, som PDL og MEDL bruker. PDL bruker `XUK` for ukjent
 * land og `XXX` for statsløs. Ukjente koder returneres uendret.
 */
fun landnavn(alpha3: String): String =
    when (alpha3) {
        "XUK" -> "ukjent land"
        "XXX" -> "statsløs"
        else -> alpha3TilAlpha2[alpha3]?.let { Locale.of("", it).getDisplayCountry(bokmål) } ?: alpha3
    }
