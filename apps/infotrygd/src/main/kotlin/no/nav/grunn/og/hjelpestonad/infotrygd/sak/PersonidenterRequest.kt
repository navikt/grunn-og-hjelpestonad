package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator

data class PersonidenterRequest(
    val personidenter: Set<String>,
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}
