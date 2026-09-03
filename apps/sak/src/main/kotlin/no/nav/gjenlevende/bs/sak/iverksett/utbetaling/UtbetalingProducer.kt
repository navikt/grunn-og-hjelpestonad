package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class UtbetalingProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val properties: UtbetalingConfigProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val topic = properties.utbetalingTopic

    fun sendUtbetaling(
        behandlingIdAsKey: String,
        melding: UtbetalingMelding,
    ) {
        log.info("Sender utbetaling til topic=$topic behandlingId=${melding.behandlingId} dryrun=${melding.dryrun}")
        kafkaTemplate.send(topic, behandlingIdAsKey, objectMapper.writeValueAsString(melding)).whenComplete { result, ex ->
            if (ex != null) {
                log.error("Feil ved sending av utbetaling behandlingId=${melding.behandlingId}", ex)
            } else {
                log.info("Utbetaling sendt behandlingId=${melding.behandlingId} offset=${result.recordMetadata.offset()}")
            }
        }
    }

    fun sendSimulering(
        behandlingId: UUID,
        melding: UtbetalingMelding,
    ) {
        val simuleringTopic = properties.simuleringResponseTopic
        log.info("Sender simulering til topic=$simuleringTopic simuleringId=$behandlingId")
        kafkaTemplate.send(simuleringTopic, behandlingId.toString(), objectMapper.writeValueAsString(melding)).whenComplete { result, ex ->
            if (ex != null) {
                log.error("Feil ved sending av simulering simuleringId=$behandlingId", ex)
            } else {
                log.info("Simulering sendt simuleringId=$behandlingId offset=${result.recordMetadata.offset()}")
            }
        }
    }
}
