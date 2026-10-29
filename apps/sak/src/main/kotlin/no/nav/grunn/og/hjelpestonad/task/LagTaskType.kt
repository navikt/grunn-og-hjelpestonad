package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.TaskType
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

/** Brukes bare til task-payloader, slik at endringer i andre mappere ikke endrer formatet på lagrede payloader. */
@PublishedApi
internal val taskPayloadMapper: ObjectMapper = jacksonObjectMapper()

/** Egen mapper i stedet for Springs, slik at TYPE kan ligge statisk i companion object. */
inline fun <reified T : Any> lagTaskType(navn: String): TaskType<T> =
    TaskType(
        navn = navn,
        serialiser = { taskPayloadMapper.writeValueAsString(it) },
        deserialiser = { taskPayloadMapper.readValue<T>(it) },
    )

/** Lagrer UUID-en som rå streng, ikke JSON, slik at payload er lik behandlingId og UNIQUE (type, payload) gir én task per behandling. */
fun lagUuidTaskType(navn: String): TaskType<UUID> =
    TaskType(
        navn = navn,
        serialiser = { it.toString() },
        deserialiser = { UUID.fromString(it) },
    )
