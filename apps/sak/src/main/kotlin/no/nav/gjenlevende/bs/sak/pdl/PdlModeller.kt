package no.nav.gjenlevende.bs.sak.pdl

import java.time.LocalDate
import java.util.UUID

data class PdlRequest(
    val query: String,
    val variables: Map<String, String>,
)

data class PdlResponseHentPersonData(
    val data: HentPersonData?,
    val errors: List<PdlError>? = null,
)

data class PdlResponseFamilierelasjoner(
    val data: FamilieRelasjonerResponse?,
    val errors: List<PdlError>? = null,
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: PdlErrorExtension? = null,
)

data class PdlErrorLocation(
    val line: Int,
    val column: Int,
)

data class PdlErrorExtension(
    val code: String? = null,
    val classification: String? = null,
)

data class HentPersonData(
    val hentPerson: HentPerson?,
)

data class HentPerson(
    val navn: List<Navn>,
    val foedselsdato: List<Foedselsdato>,
)

data class Foedselsdato(
    val foedselsdato: LocalDate,
)

data class Person(
    val navn: Navn,
    val foedselsdato: LocalDate,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class HentPersonRequest(
    val fagsakPersonId: UUID,
)

data class FamilieRelasjonerResponse(
    val hentPerson: FamilieRelasjoner?,
)

data class FamilieRelasjoner(
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String?,
    val relatertPersonsRolle: Familierolle,
    val minRolleForPerson: Familierolle?,
)

enum class Familierolle {
    BARN,
    FAR,
    MEDMOR,
    MOR,
}
