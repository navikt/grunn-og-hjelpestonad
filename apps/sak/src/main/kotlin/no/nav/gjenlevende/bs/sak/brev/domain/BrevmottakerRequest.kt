package no.nav.gjenlevende.bs.sak.brev.domain

import java.util.UUID

data class BrevmottakerRequest(
    val personRolle: BrevmottakerRolle,
    val mottakerType: MottakerType,
    val personident: String? = null,
    val orgnr: String? = null,
    val navnHosOrganisasjon: String? = null,
) {
    fun tilBrevmottaker(behandlingId: UUID) =
        Brevmottaker(
            behandlingId = behandlingId,
            personRolle = personRolle,
            mottakerType = mottakerType,
            personident = personident,
            orgnr = orgnr,
            navnHosOrganisasjon = navnHosOrganisasjon,
        )
}
