package no.nav.gjenlevende.bs.sak.unleash

import io.getunleash.Unleash
import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("integrasjonstest", "local-mock", "local-dev")
open class UnleashMock {
    @Bean
    @Primary
    open fun unleash(): Unleash {
        val mockk = mockk<Unleash>()
        every { mockk.isEnabled(any()) } returns true
        return mockk
    }

    @Bean
    @Primary
    open fun unleashService(): UnleashService {
        val mockk = mockk<UnleashService>()
        every { mockk.hentFeatureToggles() } returns
            mapOf(
                "gjenlevende_frontend__test_setup" to true,
                "gjenlevende_backend_toggle_tilgangsmaskin_i_dev" to true,
            )
        return mockk
    }
}
