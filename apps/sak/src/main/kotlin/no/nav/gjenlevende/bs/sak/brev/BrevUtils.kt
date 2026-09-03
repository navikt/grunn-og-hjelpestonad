package no.nav.gjenlevende.bs.sak.brev

import no.nav.gjenlevende.bs.sak.brev.domain.TekstbolkDto

fun hentLogoSvg(): String =
    requireNotNull(
        object {}.javaClass.classLoader.getResourceAsStream("NAV_logo_digital_Red.svg"),
    ) {
        "Fant ikke NAV_logo_digital_Red.svg i resources"
    }.use { it.bufferedReader().readText() }
        .replace("""<?xml version="1.0" encoding="UTF-8"?>""", "")
        .trim()

fun lagHtmlTekstbolker(tekstbolker: List<TekstbolkDto>): String =
    tekstbolker.joinToString(separator = "") { bolk ->
        val underoverskrift = bolk.underoverskrift?.let { "<h2>$it</h2>" } ?: ""
        "<section>\n${underoverskrift}\n<p>${bolk.innhold}</p>\n</section>\n"
    }
