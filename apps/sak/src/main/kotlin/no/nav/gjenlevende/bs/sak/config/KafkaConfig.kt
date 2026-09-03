package no.nav.gjenlevende.bs.sak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.LoggingProducerListener
import org.springframework.kafka.support.ProducerListener

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaProducerListener(): ProducerListener<Any, Any> =
        LoggingProducerListener<Any, Any>().apply {
            setIncludeContents(false)
        }
}
