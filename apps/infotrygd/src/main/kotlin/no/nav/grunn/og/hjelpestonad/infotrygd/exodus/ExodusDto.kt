package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class HentUttrekkRequest(
    val tabellnavn: String,
    val iterator: String?,
    val antallRader: Long,
)

data class HentUttrekkResponse(
    val iterator: String,
    val schema: SchemaDto,
    @JsonSetter(contentNulls = Nulls.SET)
    val innhold: List<List<String?>>,
)

data class SchemaDto(
    val kolonner: List<KolonnebeskrivelseDto>,
)

data class KolonnebeskrivelseDto(
    val navn: String,
)

fun HentUttrekkResponse.tilRader(): List<Map<String, String?>> {
    val kolonnenavn = schema.kolonner.map { it.navn.lowercase() }
    return innhold.map { rad ->
        require(rad.size == kolonnenavn.size) {
            "Rad fra Exodus har ${rad.size} verdier, men skjemaet har ${kolonnenavn.size} kolonner"
        }
        kolonnenavn.zip(rad).toMap()
    }
}

/** Exodus svarer 409 CONFLICT når Oracle har fått ny baseline. Da er alle replikerte data ugyldige. */
class NyBaselineException(
    tabell: ExodusTabell,
    cause: Throwable,
) : RuntimeException("Exodus returnerte NY_BASELINE for tabell ${tabell.tabellnavn}", cause)
