package no.nav.gjenlevende.bs.sak.felles.sikkerhet

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Aspect
@Component
class TilgangskontrollAspect(
    private val tilgangService: TilgangService,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    private val logger = LoggerFactory.getLogger(TilgangskontrollAspect::class.java)

    @Before("@annotation(tilgangskontroll)")
    fun validerTilgang(
        joinPoint: JoinPoint,
        tilgangskontroll: Tilgangskontroll,
    ) {
        val personident = finnPersonident(joinPoint)

        if (personident != null) {
            val loggMelding = tilgangskontroll.auditLogMelding.ifEmpty { joinPoint.signature.name }
            logger.info("Validerer tilgang for operasjon: $loggMelding")
            tilgangService.validerTilgangTilPersonMedRelasjoner(personident)
        } else {
            logger.warn("Kunne ikke finne personident for tilgangskontroll på metode: ${joinPoint.signature.name}")
        }
    }

    private fun finnPersonident(joinPoint: JoinPoint): String? {
        val method = (joinPoint.signature as MethodSignature).method
        val parameterNames = method.parameters.map { it.name }
        val args = joinPoint.args

        val behandlingIdIndex = parameterNames.indexOf("behandlingId")
        if (behandlingIdIndex >= 0 && args[behandlingIdIndex] is UUID) {
            val behandlingId = args[behandlingIdIndex] as UUID
            return tilgangskontrollService.hentPersonidentFraBehandling(behandlingId)
        }

        val fagsakIdIndex = parameterNames.indexOf("fagsakId")
        if (fagsakIdIndex >= 0 && args[fagsakIdIndex] is UUID) {
            val fagsakId = args[fagsakIdIndex] as UUID
            return tilgangskontrollService.hentPersonidentFraFagsak(fagsakId)
        }

        val fagsakPersonIdIndex = parameterNames.indexOf("fagsakPersonId")
        if (fagsakPersonIdIndex >= 0 && args[fagsakPersonIdIndex] is UUID) {
            val fagsakPersonId = args[fagsakPersonIdIndex] as UUID
            return tilgangskontrollService.hentPersonidentFraFagsakPerson(fagsakPersonId)
        }

        val personidentIndex = parameterNames.indexOf("personident")
        if (personidentIndex >= 0 && args[personidentIndex] is String) {
            return args[personidentIndex] as String
        }

        return null
    }
}
