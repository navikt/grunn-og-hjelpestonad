package no.nav.gjenlevende.bs.infotrygd.security

enum class Rolle(
    val beskrivelse: String,
) {
    SAKSBEHANDLER("Kan saksbehandle i saksbehandler-løsningen"),
    ATTESTERING("Kan attestere vedtak i saksbehandling-løsningen"),
    LES("Kan lese og se informasjon i saksbehandling-løsningen"),
    ;

    fun authority(): String = "ROLE_$name"

    companion object {
        val AZURE_GRUPPE_TIL_ROLLE: Map<String, Set<Rolle>> =
            mapOf(
                "5b6745de-b65d-40eb-a6f5-860c8b61c27f" to setOf(SAKSBEHANDLER),
                "70cfce24-7865-4676-9fdc-b676e90bfc92" to setOf(ATTESTERING),
                "609a78e7-e0bd-491c-a63b-96a09ec62b9b" to setOf(LES),
            )

        fun fraAzureGrupper(gruppeIder: List<String>): Set<Rolle> =
            gruppeIder
                .flatMap { gruppeId -> AZURE_GRUPPE_TIL_ROLLE[gruppeId] ?: emptySet() }
                .toSet()
    }
}
