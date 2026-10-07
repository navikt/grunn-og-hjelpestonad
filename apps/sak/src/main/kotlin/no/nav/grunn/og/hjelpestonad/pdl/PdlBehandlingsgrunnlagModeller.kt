package no.nav.grunn.og.hjelpestonad.pdl

import java.time.LocalDate
import java.time.LocalDateTime

data class PdlResponseBehandlingsgrunnlag(
    val data: HentBehandlingsgrunnlagData?,
    val errors: List<PdlError>? = null,
)

data class HentBehandlingsgrunnlagData(
    val hentPerson: PersonBehandlingsgrunnlag?,
)

data class PersonBehandlingsgrunnlag(
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus> = emptyList(),
    val bostedsadresse: List<Bostedsadresse> = emptyList(),
    val statsborgerskap: List<Statsborgerskap> = emptyList(),
    val opphold: List<Opphold> = emptyList(),
    val innflyttingTilNorge: List<InnflyttingTilNorge> = emptyList(),
    val utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList(),
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
    val doedsfall: List<Doedsfall> = emptyList(),
)

data class Metadata(
    val historisk: Boolean,
    val master: String,
)

data class Folkeregistermetadata(
    val gyldighetstidspunkt: LocalDateTime? = null,
    val opphoerstidspunkt: LocalDateTime? = null,
)

data class Folkeregisterpersonstatus(
    val status: String,
    val forenkletStatus: String,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class Bostedsadresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val angittFlyttedato: LocalDate? = null,
    val vegadresse: Vegadresse? = null,
    val matrikkeladresse: Matrikkeladresse? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
    val ukjentBosted: UkjentBosted? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class Vegadresse(
    val kommunenummer: String? = null,
)

data class Matrikkeladresse(
    val kommunenummer: String? = null,
)

data class UtenlandskAdresse(
    val landkode: String,
)

data class UkjentBosted(
    val bostedskommune: String? = null,
)

data class Statsborgerskap(
    val land: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val bekreftelsesdato: LocalDate? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class Opphold(
    val type: String,
    val oppholdFra: LocalDate? = null,
    val oppholdTil: LocalDate? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class InnflyttingTilNorge(
    val fraflyttingsland: String? = null,
    val fraflyttingsstedIUtlandet: String? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class UtflyttingFraNorge(
    val tilflyttingsland: String? = null,
    val tilflyttingsstedIUtlandet: String? = null,
    val utflyttingsdato: LocalDate? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class Oppholdsadresse(
    val gyldigFraOgMed: LocalDateTime? = null,
    val gyldigTilOgMed: LocalDateTime? = null,
    val utenlandskAdresse: UtenlandskAdresse? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)

data class Doedsfall(
    val doedsdato: LocalDate? = null,
    val metadata: Metadata,
    val folkeregistermetadata: Folkeregistermetadata? = null,
)
