package no.nav.gjenlevende.bs.sak.iverksett.utbetaling

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.stereotype.Component

@Component
class HelvedStatusFagsystemHeaderFilter : RecordFilterStrategy<String, String> {
    /**
     * Filtrerer bort meldinger som ikke har "fagsystem" header satt til "GJENLEVENDE".
     * @return true hvis meldingen skal forkastes
     */
    override fun filter(consumerRecord: ConsumerRecord<String, String>): Boolean {
        val fagsystemHeader = consumerRecord.headers().lastHeader("fagsystem")
        return fagsystemHeader == null || "GJENLEVENDE" != String(fagsystemHeader.value()) // TODO koordiner navn på fagsystem
    }
}
