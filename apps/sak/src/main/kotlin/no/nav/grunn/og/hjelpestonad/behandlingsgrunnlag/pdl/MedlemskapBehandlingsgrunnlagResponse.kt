package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.util.landnavn
import java.time.LocalDate
import java.time.LocalDateTime

data class MedlemskapBehandlingsgrunnlagResponse(
    val hentetTidspunkt: LocalDateTime?,
    val bosted: List<OpplysningResponse>,
    val statsborgerskap: List<OpplysningResponse>,
    val oppholdstillatelse: List<OpplysningResponse>,
)

data class OpplysningResponse(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
    val beskrivelse: String,
)

fun PdlBehandlingsgrunnlag?.tilMedlemskapBehandlingsgrunnlagResponse(): MedlemskapBehandlingsgrunnlagResponse {
    if (this == null) {
        return MedlemskapBehandlingsgrunnlagResponse(
            hentetTidspunkt = null,
            bosted = emptyList(),
            statsborgerskap = emptyList(),
            oppholdstillatelse = emptyList(),
        )
    }

    val adresser =
        bostedsadresse.map {
            OpplysningResponse(
                fraOgMedDato = it.gyldigFraOgMed?.toLocalDate() ?: it.angittFlyttedato ?: it.metadata.gyldighetstidspunkt?.toLocalDate(),
                tilOgMedDato = it.gyldigTilOgMed?.toLocalDate(),
                beskrivelse = it.beskrivelse(),
            )
        }
    val personstatuser =
        folkeregisterpersonstatus.map {
            OpplysningResponse(
                fraOgMedDato = it.metadata.gyldighetstidspunkt?.toLocalDate(),
                tilOgMedDato = it.metadata.opphoerstidspunkt?.toLocalDate(),
                beskrivelse = "Personstatus: ${personstatusTekst(it.status)}",
            )
        }
    val innflyttinger =
        innflyttingTilNorge.map {
            val dato = it.metadata.gyldighetstidspunkt?.toLocalDate()
            OpplysningResponse(
                fraOgMedDato = dato,
                tilOgMedDato = dato,
                beskrivelse = "Innflyttet fra ${stedTekst(it.fraflyttingsland, it.fraflyttingsstedIUtlandet)}",
            )
        }
    val utflyttinger =
        utflyttingFraNorge.map {
            val dato = it.utflyttingsdato ?: it.metadata.gyldighetstidspunkt?.toLocalDate()
            OpplysningResponse(
                fraOgMedDato = dato,
                tilOgMedDato = dato,
                beskrivelse = "Utflyttet til ${stedTekst(it.tilflyttingsland, it.tilflyttingsstedIUtlandet)}",
            )
        }

    return MedlemskapBehandlingsgrunnlagResponse(
        hentetTidspunkt = hentetTidspunkt,
        bosted = (adresser + personstatuser + innflyttinger + utflyttinger).nyesteFørst(),
        statsborgerskap =
            statsborgerskap
                .map {
                    OpplysningResponse(
                        fraOgMedDato = it.gyldigFraOgMed,
                        tilOgMedDato = it.gyldigTilOgMed,
                        beskrivelse = landnavn(it.land).replaceFirstChar(Char::uppercase),
                    )
                }.nyesteFørst(),
        oppholdstillatelse =
            opphold
                .map {
                    OpplysningResponse(
                        fraOgMedDato = it.oppholdFra,
                        tilOgMedDato = it.oppholdTil,
                        beskrivelse = oppholdTekst(it.type),
                    )
                }.nyesteFørst(),
    )
}

private fun List<OpplysningResponse>.nyesteFørst() = sortedWith(compareByDescending(nullsFirst()) { it.fraOgMedDato })

private fun PdlBostedsadresse.beskrivelse() =
    when (adressetype) {
        PdlAdressetype.VEGADRESSE, PdlAdressetype.MATRIKKELADRESSE -> "Bostedsadresse i Norge"
        PdlAdressetype.UTENLANDSK_ADRESSE -> "Bostedsadresse i ${landkode?.let(::landnavn) ?: "utlandet"}"
        PdlAdressetype.UKJENT_BOSTED -> "Ukjent bosted"
        null -> "Bostedsadresse uten type"
    }

private fun stedTekst(
    land: String?,
    sted: String?,
): String {
    val landTekst = land?.let(::landnavn) ?: "ukjent land"
    return if (sted.isNullOrBlank()) landTekst else "$landTekst ($sted)"
}

private fun personstatusTekst(status: String) =
    when (status) {
        "bosatt" -> "bosatt"
        "utflyttet" -> "utflyttet"
        "forsvunnet" -> "forsvunnet"
        "doed" -> "død"
        "opphoert" -> "opphørt"
        "foedselsregistrert" -> "fødselsregistrert"
        "ikkeBosatt" -> "ikke bosatt"
        "midlertidig" -> "midlertidig (D-nummer)"
        "inaktiv" -> "inaktiv (D-nummer)"
        "aktiv" -> "aktiv (D-nummer)"
        else -> status
    }

private fun oppholdTekst(type: String) =
    when (type) {
        "MIDLERTIDIG" -> "Midlertidig oppholdstillatelse"
        "PERMANENT" -> "Permanent oppholdstillatelse"
        "OPPLYSNING_MANGLER" -> "Opplysning om oppholdstillatelse mangler"
        else -> type
    }
