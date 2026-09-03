package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class SimuleringResponseListener(
    private val objectMapper: ObjectMapper,
    private val simuleringRepository: SimuleringRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /*
    @KafkaListener(
        topics = ["\${app.utbetaling.simulering-response-topic}"],
        groupId = "gjenlevende-bs-sak.simulering",
    )
     */
    fun mottaSimuleringsresultat(record: ConsumerRecord<String, String>) {
        val simuleringId =
            runCatching { UUID.fromString(record.key()) }.getOrElse {
                log.error("Ugyldig simuleringId i Kafka-key: ${record.key()}")
                return
            }

        val simulering = simuleringRepository.findById(simuleringId).orElse(null)
        if (simulering == null) {
            log.warn("Mottok simuleringssvar for ukjent simuleringId=$simuleringId")
            return
        }

        runCatching {
            val respons = objectMapper.readValue(record.value(), SimuleringResponse::class.java)
            simuleringRepository.update(simulering.copy(status = SimuleringStatus.FERDIG, respons = respons))
            log.info("Simuleringssvar mottatt og lagret for simuleringId=$simuleringId")
        }.onFailure { ex ->
            log.error("Feil ved parsing av simuleringssvar for simuleringId=$simuleringId", ex)
            simuleringRepository.update(simulering.copy(status = SimuleringStatus.FEILET))
        }
    }
}
