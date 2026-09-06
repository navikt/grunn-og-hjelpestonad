package no.nav.grunn.og.hjelpestonad.unleash

enum class FeatureToggle(
    val toggleName: String,
) {
    TEST_SETUP("grunn-og-hjelp_frontend__test_setup"),
    TOGGLE_TILGANGSMASKIN_I_DEV("grunn-og-hjelp_backend_toggle_tilgangsmaskin_i_dev"),
    ;

    companion object {
        fun hentAlleFeatureToggleNavn(): List<String> = entries.map { it.toggleName }
    }
}
