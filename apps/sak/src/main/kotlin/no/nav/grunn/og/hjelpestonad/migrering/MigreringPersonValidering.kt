package no.nav.grunn.og.hjelpestonad.migrering

import no.nav.grunn.og.hjelpestonad.pdl.PersonIdentitetOgDoedsfall

enum class MigreringPersonBlokkering {
    DØD,
    FALSK_IDENTITET,
}

data class MigreringPersonValideringsresultat(
    val blokkeringer: Set<MigreringPersonBlokkering>,
) {
    val kanMigreres: Boolean
        get() = blokkeringer.isEmpty()
}

/**
 * Deterministisk validering for om en person kan migreres basert på opplysninger fra PDL.
 * Ren funksjon uten Spring-avhengigheter (jf. ADR-0008).
 */
object MigreringPersonValidering {
    fun valider(personIdentitetOgDoedsfall: PersonIdentitetOgDoedsfall): MigreringPersonValideringsresultat {
        val blokkeringer =
            setOfNotNull(
                MigreringPersonBlokkering.DØD.takeIf {
                    personIdentitetOgDoedsfall.doedsfall.isNotEmpty()
                },
                MigreringPersonBlokkering.FALSK_IDENTITET.takeIf {
                    personIdentitetOgDoedsfall.falskIdentitet?.erFalsk == true
                },
            )
        return MigreringPersonValideringsresultat(blokkeringer = blokkeringer)
    }
}
