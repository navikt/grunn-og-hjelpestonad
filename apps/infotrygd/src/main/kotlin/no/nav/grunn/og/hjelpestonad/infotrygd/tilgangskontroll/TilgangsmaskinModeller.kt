package no.nav.grunn.og.hjelpestonad.infotrygd.tilgangskontroll

enum class Regeltype { KJERNE_REGELTYPE, KOMPLETT_REGELTYPE }

enum class Avvisningskode(
    val feilmelding: String,
) {
    AVVIST_STRENGT_FORTROLIG_ADRESSE("Du har ikke tilgang til brukere med strengt fortrolig adresse (kode 6)."),
    AVVIST_STRENGT_FORTROLIG_UTLAND("Du har ikke tilgang til brukere med strengt fortrolig adresse i utlandet (§19)."),
    AVVIST_FORTROLIG_ADRESSE("Du har ikke tilgang til brukere med fortrolig adresse (kode 7)."),
    AVVIST_SKJERMING("Du har ikke tilgang til Nav-ansatte og deres nærmeste familie."),
    AVVIST_HABILITET("Du har ikke tilgang til data om deg selv eller dine nærstående."),
    AVVIST_VERGE("Du har ikke tilgang fordi du er registrert som brukerens verge."),
    AVVIST_GEOGRAFISK("Du har ikke tilgang til brukerens geografiske område eller enhet."),
    AVVIST_AVDØD(
        "Du har ikke tilgang til brukere som har vært død mer enn 12 måneder. " +
            "Du kan be om tilgangen «Populasjon avdød bruker» gjennom Mine Tilganger.",
    ),
    AVVIST_PERSON_UTLAND("Du har ikke tilgang til brukere som bor i utlandet."),
    AVVIST_UKJENT_BOSTED("Du har ikke tilgang til brukere uten kjent bosted."),
}

data class Tilgangsvurdering(
    val personident: String,
    val harTilgang: Boolean,
    val avvisningskode: Avvisningskode?,
    val traceId: String? = null,
)

data class BrukerIdOgRegeltypeDto(
    val brukerId: String,
    val type: Regeltype,
)

data class BulkResponsDto(
    val ansattId: String? = null,
    val resultater: List<BulkResultatDto> = emptyList(),
)

data class BulkResultatDto(
    val brukerId: String,
    val status: Int,
    val detaljer: DetaljerDto? = null,
)

data class DetaljerDto(
    val title: String? = null,
    val begrunnelse: String? = null,
    val traceId: String? = null,
)
