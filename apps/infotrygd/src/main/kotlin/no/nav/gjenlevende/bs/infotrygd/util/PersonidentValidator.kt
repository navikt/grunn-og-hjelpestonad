package no.nav.gjenlevende.bs.infotrygd.util

object PersonidentValidator {
    private val PERSONIDENT_REGEX = """[0-9]{11}""".toRegex()

    fun validerPersonident(personident: String) {
        if (personident.length != 11) {
            throw IllegalArgumentException("Ugyldig personident")
        }
        if (!PERSONIDENT_REGEX.matches(personident)) {
            throw IllegalArgumentException("Ugyldig personident. Det kan kun inneholde tall")
        }
    }
}
