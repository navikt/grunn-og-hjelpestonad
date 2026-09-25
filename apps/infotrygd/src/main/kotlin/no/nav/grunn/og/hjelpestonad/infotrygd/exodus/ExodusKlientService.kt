package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class SideResultat {
    FLERE_SIDER,
    AJOUR,
    NY_BASELINE,
}

/**
 * Replikerer én side for en tabell: les iterator, hent rader fra Exodus, og lagre rader og ny
 * iterator i samme transaksjon. Feiler noe, committes ingenting og siden hentes på nytt neste gang.
 */
@Service
open class ExodusKlientService(
    private val exodusClient: ExodusClient,
    private val statusRepository: ExodusStatusRepository,
    private val skrivService: ExodusSkrivService,
    private val properties: ExodusProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    open fun replikerNesteSide(tabell: ExodusTabell): SideResultat {
        val iterator = statusRepository.finn(tabell.tabellnavn)?.iterator

        val respons =
            try {
                exodusClient.hentUttrekk(tabell, iterator, properties.batchStorrelse)
            } catch (e: NyBaselineException) {
                logger.warn("Ny baseline i Exodus (oppdaget på ${tabell.tabellnavn}). Tømmer alle tabeller og starter på nytt.", e)
                skrivService.nullstillAlleTabeller()
                return SideResultat.NY_BASELINE
            }

        val rader = respons.tilRader()
        val flereSider = rader.size >= properties.batchStorrelse
        skrivService.lagreSide(tabell, rader, respons.iterator.ifBlank { iterator }, flereSider)
        logger.debug("Tabell ${tabell.tabellnavn}: lagret ${rader.size} rader")

        return if (flereSider) SideResultat.FLERE_SIDER else SideResultat.AJOUR
    }
}
