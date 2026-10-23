package no.nav.grunn.og.hjelpestonad.infotrygd.sak

import no.nav.grunn.og.hjelpestonad.infotrygd.util.PersonidentValidator

data class PersonidenterRequest(
    val personidenter: Set<String>,
) {
    fun valider() {
        require(personidenter.isNotEmpty()) { "Må oppgi minst én personident" }
        require(personidenter.size == 1) { "Kan bare slå opp én personident om gangen" }
        personidenter.forEach(PersonidentValidator::validerPersonident)
    }
}
