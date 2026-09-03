package no.nav.gjenlevende.bs.sak.config

import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.gjenlevende.bs.sak.beslutter.ÅrsakUnderkjent
import no.nav.gjenlevende.bs.sak.brev.domain.BrevRequest
import no.nav.gjenlevende.bs.sak.iverksett.utbetaling.SimuleringResponse
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.time.YearMonth
import javax.sql.DataSource

@Configuration
@EnableJdbcRepositories("no.nav.familie", "no.nav.gjenlevende")
open class DatabaseConfig(
    private val objectMapper: ObjectMapper,
) : AbstractJdbcConfiguration() {
    @Bean
    open fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)

    override fun userConverters(): List<*> =
        listOf(
            PropertiesWrapperTilStringConverter(),
            StringTilPropertiesWrapperConverter(),
            BrevRequestTilJsonbConverter(objectMapper),
            JsonbTilBrevRequestConverter(objectMapper),
            SimuleringResponseTilJsonbConverter(objectMapper),
            JsonbTilSimuleringResponseConverter(objectMapper),
            StringTilYearMonthConverter(),
        )

    @Bean
    open fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile men har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} men har ignoreIfProd=--")
            }
        }
    }

    @WritingConverter
    class BrevRequestTilJsonbConverter(
        private val objectMapper: ObjectMapper,
    ) : Converter<BrevRequest, PGobject> {
        override fun convert(source: BrevRequest): PGobject =
            PGobject().apply {
                type = "jsonb"
                value = objectMapper.writeValueAsString(source)
            }
    }

    @ReadingConverter
    class JsonbTilBrevRequestConverter(
        private val objectMapper: ObjectMapper,
    ) : Converter<PGobject, BrevRequest> {
        override fun convert(source: PGobject): BrevRequest = objectMapper.readValue(source.value, BrevRequest::class.java)
    }

    @ReadingConverter
    class StringTilYearMonthConverter : Converter<String, YearMonth> {
        override fun convert(source: String): YearMonth = YearMonth.parse(source)
    }

    @WritingConverter
    class SimuleringResponseTilJsonbConverter(
        private val objectMapper: ObjectMapper,
    ) : Converter<SimuleringResponse, PGobject> {
        override fun convert(source: SimuleringResponse): PGobject =
            PGobject().apply {
                type = "jsonb"
                value = objectMapper.writeValueAsString(source)
            }
    }

    @ReadingConverter
    class JsonbTilSimuleringResponseConverter(
        private val objectMapper: ObjectMapper,
    ) : Converter<PGobject, SimuleringResponse> {
        override fun convert(source: PGobject): SimuleringResponse = objectMapper.readValue(source.value, SimuleringResponse::class.java)
    }
}
