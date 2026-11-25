package no.nav.grunn.og.hjelpestonad.pdl

import java.time.LocalDate

data class PdlResponseIdentitetOgDoedsfall(
    val data: HentIdentitetOgDoedsfallData?,
    val errors: List<PdlError>? = null,
)

data class HentIdentitetOgDoedsfallData(
    val hentPerson: PersonIdentitetOgDoedsfall?,
)

data class PersonIdentitetOgDoedsfall(
    val falskIdentitet: FalskIdentitetEnkel? = null,
    val doedsfall: List<DoedsfallEnkel> = emptyList(),
)

data class FalskIdentitetEnkel(
    val erFalsk: Boolean,
)

data class DoedsfallEnkel(
    val doedsdato: LocalDate? = null,
)
