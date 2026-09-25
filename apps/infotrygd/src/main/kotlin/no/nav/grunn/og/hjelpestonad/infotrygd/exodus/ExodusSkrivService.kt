package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Egen bean slik at @Transactional går gjennom Spring-proxyen. HTTP-kallet mot Exodus skjer utenfor transaksjonen. */
@Service
open class ExodusSkrivService(
    private val upsertRepository: ExodusUpsertRepository,
    private val statusRepository: ExodusStatusRepository,
) {
    @Transactional
    open fun lagreSide(
        tabell: ExodusTabell,
        rader: List<Map<String, String?>>,
        iterator: String?,
        flereSider: Boolean,
    ) {
        upsertRepository.upsert(tabell, rader)
        statusRepository.oppdaterIterator(tabell.tabellnavn, iterator, rader.size, flereSider)
    }

    /** Ved ny baseline er alle replikerte data ugyldige, så alle tabeller tømmes og starter på nytt. */
    @Transactional
    open fun nullstillAlleTabeller() {
        upsertRepository.truncate(ExodusTabell.entries)
        statusRepository.settNyBaseline(ExodusTabell.entries.map { it.tabellnavn })
    }
}
