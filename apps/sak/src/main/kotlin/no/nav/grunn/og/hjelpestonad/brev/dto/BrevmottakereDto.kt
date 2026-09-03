package no.nav.grunn.og.hjelpestonad.brev.dto

import no.nav.grunn.og.hjelpestonad.brev.domain.Brevmottaker
import no.nav.grunn.og.hjelpestonad.brev.domain.BrevmottakerRolle
import no.nav.grunn.og.hjelpestonad.brev.domain.MottakerType

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
