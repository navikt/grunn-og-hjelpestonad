package no.nav.grunn.og.hjelpestonad.security

enum class Rolle(
    val beskrivelse: String,
) {
    SAKSBEHANDLER("Kan saksbehandle i saksbehandler-løsningen"),
    BESLUTTER("Kan beslutte vedtak i saksbehandling-løsningen"),
    LESETILGANG("Kan lese og se informasjon i saksbehandling-løsningen"),
    PROSESSERING("Kan behandle tasks i efterlatte-verktoy"),
    ;

    fun authority(): String = "ROLE_$name"

    companion object {
        val AZURE_GRUPPE_TIL_ROLLE: Map<String, Set<Rolle>> =
            mapOf(
                "7ce9d1d2-d149-4324-832b-8d459762a102" to setOf(SAKSBEHANDLER),
                "84c4a287-abd6-46c1-bf93-dbf90f1a326d" to setOf(BESLUTTER),
                "a181921e-2a55-4198-896b-0086cc805278" to setOf(LESETILGANG),
                "928636f4-fd0d-4149-978e-a6fb68bb19de" to setOf(PROSESSERING),
            )

        fun fraAzureGrupper(gruppeIder: List<String>): Set<Rolle> =
            gruppeIder
                .flatMap { gruppeId -> AZURE_GRUPPE_TIL_ROLLE[gruppeId] ?: emptySet() }
                .toSet()
    }
}
