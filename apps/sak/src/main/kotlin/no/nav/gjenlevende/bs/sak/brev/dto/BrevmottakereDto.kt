package no.nav.gjenlevende.bs.sak.brev.dto

import no.nav.gjenlevende.bs.sak.brev.domain.Brevmottaker
import no.nav.gjenlevende.bs.sak.brev.domain.BrevmottakerRolle
import no.nav.gjenlevende.bs.sak.brev.domain.MottakerType

data class BrevmottakerDto(
    val personRolle: BrevmottakerRolle,
    val mottakerType: MottakerType,
    val personident: String? = null,
    val orgnr: String? = null,
    val navnHosOrganisasjon: String? = null,
)

fun Brevmottaker.tilDto() =
    BrevmottakerDto(
        personRolle = this.personRolle,
        mottakerType = this.mottakerType,
        personident = this.personident,
        orgnr = this.orgnr,
        navnHosOrganisasjon = this.navnHosOrganisasjon,
    )
