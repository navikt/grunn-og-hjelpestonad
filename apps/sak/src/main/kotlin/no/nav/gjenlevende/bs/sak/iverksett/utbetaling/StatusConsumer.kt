package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class StatusConsumer(
    private val objectMapper: ObjectMapper,
    properties: UtbetalingConfigProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Starter status consumer for topic: ${properties.statusTopic}")
    }

    /*
    @KafkaListener(
        topics = ["\${app.utbetaling.status-topic}"],
        groupId = "gjenlevende-bs-sak.status",
        filter = "helvedStatusFagsystemHeaderFilter",
    )
     */
    fun mottaStatus(record: ConsumerRecord<String, String>) {
        try {
            val status = objectMapper.readValue(record.value(), UtbetalingStatusMelding::class.java)
            log.info("Mottatt status fra helved: key=${record.key()} status=${status.status}")
            if (status.status == StatusType.FEILET) {
                log.error("Utbetaling feilet: key=${record.key()} error=${status.error}")
            }
        } catch (e: Exception) {
            log.error("Feil ved parsing av statusmelding: key=${record.key()}", e)
        }
    }
}
