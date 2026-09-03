package no.nav.familie.leader

object LeaderEnvironment {
    @JvmStatic
    fun hentLeaderSystemEnv(): String? {
        return System.getenv("ELECTOR_PATH") ?: return null
    }
}
