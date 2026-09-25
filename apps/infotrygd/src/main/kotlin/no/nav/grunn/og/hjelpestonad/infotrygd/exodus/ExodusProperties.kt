package no.nav.grunn.og.hjelpestonad.infotrygd.exodus

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "exodus")
data class ExodusProperties(
    val baseUrl: String,
    val scope: String,
    val batchStorrelse: Int = 1000,
    val schedulerCron: String = "0 */5 * * * *",
    val maksSiderPerKjoring: Int = 500,
    val schedulerEnabled: Boolean = false,
)
