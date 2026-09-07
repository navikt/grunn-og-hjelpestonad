package no.nav.grunn.og.hjelpestonad.felles.sikkerhet

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tilgangskontroll(
    val auditLogMelding: String = "",
)
