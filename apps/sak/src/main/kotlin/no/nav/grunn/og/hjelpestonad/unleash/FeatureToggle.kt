package no.nav.grunn.og.hjelpestonad.unleash

enum class FeatureToggle(
    val toggleName: String,
) {
    TEST_SETUP("grunn-og-hjelp_frontend__test_setup"),
    ;

    companion object {
        fun hentAlleFeatureToggleNavn(): List<String> = entries.map { it.toggleName }
    }
}
