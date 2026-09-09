package no.nav.grunn.og.hjelpestonad.security

enum class Rolle(
    val beskrivelse: String,
) {
    SAKSBEHANDLER("Kan saksbehandle i saksbehandler-løsningen"),
    BESLUTTER("Kan beslutte vedtak i saksbehandling-løsningen"),
    LES("Kan lese og se informasjon i saksbehandling-løsningen"),
    ;

    fun authority(): String = "ROLE_$name"

    companion object {
        val AZURE_GRUPPE_TIL_ROLLE: Map<String, Set<Rolle>> =
            mapOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102" to setOf(SAKSBEHANDLER),
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d" to setOf(BESLUTTER),
                "a181921e-2a55-4198-896b-0086cc805278" to setOf(LES),
            )

        fun fraAzureGrupper(gruppeIder: List<String>): Set<Rolle> =
            gruppeIder
                .flatMap { gruppeId -> AZURE_GRUPPE_TIL_ROLLE[gruppeId] ?: emptySet() }
                .toSet()
    }
}
