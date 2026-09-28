package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
open class ExodusScheduler(
    private val klientService: ExodusKlientService,
    private val lederVelger: LederVelger,
    private val properties: ExodusProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Med virtuelle tråder bruker Spring SimpleAsyncTaskScheduler, som starter cron-jobber på nytt selv om
     * forrige kjøring fortsatt pågår. Uten denne sperren jobber flere kjøringer på samme tabell samtidig.
     */
    private val kjører = AtomicBoolean(false)

    @Scheduled(cron = "\${exodus.scheduler-cron}")
    open fun replikerAlleTabeller() {
        if (!properties.schedulerEnabled || !lederVelger.erLeder()) return
        if (!kjører.compareAndSet(false, true)) {
            logger.info("Forrige replikering fra Exodus pågår fortsatt. Hopper over denne kjøringen.")
            return
        }
        try {
            replikerTabellene()
        } finally {
            kjører.set(false)
        }
    }

    private fun replikerTabellene() {
        logger.info("Starter replikering av ${ExodusTabell.entries.size} tabeller fra Exodus")
        for (tabell in ExodusTabell.entries) {
            if (replikerTabell(tabell) == SideResultat.NY_BASELINE) {
                logger.info("Avbryter kjøringen etter ny baseline. Alle tabeller replikeres på nytt neste kjøring.")
                return
            }
        }
        logger.info("Replikering fra Exodus ferdig")
    }

    /** Henter sider til tabellen er ajour eller sidetaket er nådd. Feil logges, og tabellen prøves igjen neste kjøring. */
    private fun replikerTabell(tabell: ExodusTabell): SideResultat? =
        try {
            val resultater =
                generateSequence { klientService.replikerNesteSide(tabell) }
                    .take(properties.maksSiderPerKjoring)
                    .takeWhileInclusive { it == SideResultat.FLERE_SIDER }
                    .toList()
            logger.info("Tabell ${tabell.tabellnavn}: ${resultater.size} sider hentet, status ${resultater.lastOrNull()}")
            resultater.lastOrNull()
        } catch (e: Exception) {
            logger.error("Replikering av tabell ${tabell.tabellnavn} feilet. Prøver igjen neste kjøring.", e)
            null
        }
}

private fun <T> Sequence<T>.takeWhileInclusive(predikat: (T) -> Boolean): Sequence<T> =
    sequence {
        for (element in this@takeWhileInclusive) {
            yield(element)
            if (!predikat(element)) break
        }
    }
