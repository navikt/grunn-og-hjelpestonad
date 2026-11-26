package no.nav.grunn.og.hjelpestonad.pdl

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class PdlResponsePersonopplysningerForMigrering(
    val data: HentPersonopplysningerForMigreringData?,
    val errors: List<PdlError>? = null,
)

data class HentPersonopplysningerForMigreringData(
    val hentPerson: PersonopplysningerForMigrering?,
)

data class PersonopplysningerForMigrering(
    val falskIdentitet: FalskIdentitetEnkel? = null,
    @JsonProperty("doedsfall")
    val dødsfall: List<DødsfallEnkel> = emptyList(),
)

data class FalskIdentitetEnkel(
    val erFalsk: Boolean,
)

data class DødsfallEnkel(
    @JsonProperty("doedsdato")
    val dødsdato: LocalDate? = null,
)
