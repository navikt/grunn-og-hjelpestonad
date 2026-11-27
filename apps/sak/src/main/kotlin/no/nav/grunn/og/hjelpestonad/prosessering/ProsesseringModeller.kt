package no.nav.grunn.og.hjelpestonad.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import efterlatte.prosessering.TaskStateMachine

data class ProsesseringTaskSide(
    val tasks: List<Task>,
    val side: Int,
    val antallPerSide: Int,
    val antallTotalt: Long,
    val antallSider: Long,
    val avkortet: Boolean = false,
)

data class ProsesseringStatusTelling(
    val status: Status,
    val antall: Long,
)

data class ProsesseringTypeTelling(
    val type: String,
    val antall: Long,
)

data class ProsesseringEndring(
    val versjon: Long,
)

data class ProsesseringAvvik(
    val versjon: Long,
    val melding: String,
)

enum class ProsesseringHandling(
    val nyStatus: Status,
) {
    REKJOR(Status.KLAR),
    AVBRYT(Status.AVBRUTT),
    AVVIK(Status.AVBRUTT),
    ;

    fun erTillattFra(status: Status): Boolean = status != Status.KJØRER && TaskStateMachine.erLovlig(status, nyStatus)
}
