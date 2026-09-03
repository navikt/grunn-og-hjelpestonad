package no.nav.gjenlevende.bs.sak.tilgangskontroll

import com.fasterxml.jackson.annotation.JsonProperty

enum class RegelType {
    @JsonProperty("KOMPLETT_REGELTYPE")
    KOMPLETT_REGELTYPE,

    @JsonProperty("KJERNE_REGELTYPE")
    KJERNE_REGELTYPE,
}

enum class Avvisningskode {
    AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AVVIST_STRENGT_FORTROLIG_UTLAND,
    AVVIST_AVDØD,
    AVVIST_PERSON_UTLAND,
    AVVIST_SKJERMING,
    AVVIST_FORTROLIG_ADRESSE,
    AVVIST_UKJENT_BOSTED,
    AVVIST_GEOGRAFISK,
    AVVIST_HABILITET,
}

data class TilgangssjekkRequest(
    val personident: String,
)

data class AnsattInfoRequest(
    val navIdent: String,
)

data class BulkTilgangsRequest(
    val personidenter: List<String>,
)

data class BulkTilgangsResponse(
    @JsonProperty("ansattId")
    val navIdent: String,
    val resultater: Set<TilgangsResultat> = emptySet(),
)

data class TilgangsResultat(
    @JsonProperty("brukerId")
    val personident: String,
    val status: Int,
    val detaljer: Any? = null,
) {
    val harTilgang: Boolean
        get() = status == 204
}

data class ForenkletBulkTilgangsResponse(
    val navIdent: String,
    val resultater: List<ForenkletTilgangsResultat>,
)

data class ForenkletTilgangsResultat(
    val personident: String,
    val harTilgang: Boolean,
    val avvisningskode: Avvisningskode? = null,
    val begrunnelse: String? = null,
)

data class EnkelTilgangsResponse(
    val navIdent: String,
    val personident: String,
    val harTilgang: Boolean,
    val avvisningskode: Avvisningskode? = null,
    val begrunnelse: String? = null,
)

data class AnsattInfoResponse(
    @JsonProperty("ansattId")
    val navIdent: String,
    val bruker: BrukerInfo? = null,
    val grupper: List<GruppeInfo> = emptyList(),
)

data class GruppeInfo(
    val id: String,
    val displayName: String = "N/A",
)

data class BrukerInfo(
    val brukerIds: BrukerIds,
    val geografiskTilknytning: GeografiskTilknytning? = null,
    val familie: FamilieInfo? = null,
    val harUkjentBosted: Boolean = false,
    val harUtenlandskBosted: Boolean = false,
    val oppslagId: String,
) {
    val kommunenummer: String?
        get() = geografiskTilknytning?.kommune?.verdi
}

data class BrukerIds(
    @JsonProperty("aktivBrukerId")
    val aktivPersonident: String,
    val oppslagId: String,
    val historiskeIds: List<String> = emptyList(),
)

data class GeografiskTilknytning(
    val kommune: KommuneInfo? = null,
)

data class KommuneInfo(
    val verdi: String,
)

data class FamilieInfo(
    val foreldre: List<FamilieRelasjon> = emptyList(),
    val barn: List<FamilieRelasjon> = emptyList(),
    val partnere: List<FamilieRelasjon> = emptyList(),
)

data class FamilieRelasjon(
    @JsonProperty("brukerId")
    val personident: String,
    val relasjon: String,
)

data class TilgangsmaskinFeilResponse(
    val title: String? = null,
    val begrunnelse: String? = null,
)
