package no.nav.grunn.og.hjelpestonad.migrering

import no.nav.grunn.og.hjelpestonad.pdl.PersonopplysningerForMigrering

enum class PersonblokkeringForMigrering {
    DØD,
    FALSK_IDENTITET,
}

data class PersonopplysningerForMigreringValideringResultat(
    val blokkeringer: Set<PersonblokkeringForMigrering>,
) {
    val kanMigreres: Boolean
        get() = blokkeringer.isEmpty()
}

/**
 * Deterministisk validering for om en person kan migreres basert på opplysninger fra PDL.
 * Ren funksjon uten Spring-avhengigheter (jf. ADR-0008).
 */
object MigreringPersonValidering {
    fun valider(personopplysningerForMigrering: PersonopplysningerForMigrering): PersonopplysningerForMigreringValideringResultat {
        val blokkeringer =
            setOfNotNull(
                PersonblokkeringForMigrering.DØD.takeIf {
                    personopplysningerForMigrering.dødsfall.isNotEmpty()
                },
                PersonblokkeringForMigrering.FALSK_IDENTITET.takeIf {
                    personopplysningerForMigrering.falskIdentitet?.erFalsk == true
                },
            )
        return PersonopplysningerForMigreringValideringResultat(blokkeringer = blokkeringer)
    }
}
