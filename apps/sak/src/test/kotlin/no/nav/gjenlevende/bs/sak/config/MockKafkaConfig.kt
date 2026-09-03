package no.nav.gjenlevende.bs.sak.config

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate

@TestConfiguration
@Profile("integrasjonstest")
class MockKafkaConfig {
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = mockk(relaxed = true)
}
