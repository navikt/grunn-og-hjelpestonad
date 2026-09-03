package no.nav.gjenlevende.bs.sak.oppgave.dto

enum class SaksbehandlerRolle {
    INNLOGGET_SAKSBEHANDLER,
    ANNEN_SAKSBEHANDLER,
    IKKE_SATT,
}

data class AnsvarligSaksbehandlerDto(
    val fornavn: String,
    val etternavn: String,
    val rolle: SaksbehandlerRolle,
)
