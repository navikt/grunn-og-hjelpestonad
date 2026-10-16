package no.nav.grunn.og.hjelpestonad.unleash

import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("integrasjonstest", "local-mock")
open class UnleashMock {
    @Bean
    @Primary
    open fun unleash(): Unleash = FakeUnleash().apply { enableAll() }
}
