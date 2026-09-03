package no.nav.gjenlevende.bs.infotrygd.config

import jakarta.validation.constraints.NotEmpty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(DatasourceConfiguration::class)
open class DatasourceConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    open fun vaultDatasourceUsername(
        @Value("\${vault.username}") filePath: String,
    ): String {
        val path = Paths.get(filePath)
        val username = Files.readString(path)
        return username
    }

    @Bean
    open fun vaultDatasourcePassword(
        @Value("\${vault.password}") filePath: String,
    ): String {
        val path = Paths.get(filePath)
        return Files.readString(path)
    }

    @Bean
    open fun datasource(
        datasourceConfiguration: DatasourceConfiguration,
        vaultDatasourceUsername: String,
        vaultDatasourcePassword: String,
    ): DataSource {
        requireNotNull(datasourceConfiguration.url) { "spring.datasource.url is null" }
        requireNotNull(datasourceConfiguration.driverClassName) { "spring.datasource.driverClassName is null" }
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName(datasourceConfiguration.driverClassName!!)
        dataSourceBuilder.url(datasourceConfiguration.url!!)
        dataSourceBuilder.username(vaultDatasourceUsername)
        dataSourceBuilder.password(vaultDatasourcePassword)
        return dataSourceBuilder.build()
    }
}

@ConfigurationProperties(prefix = "spring.datasource")
@Validated
data class DatasourceConfiguration(
    @NotEmpty
    var url: String? = null,
    @NotEmpty
    var driverClassName: String? = null,
)
