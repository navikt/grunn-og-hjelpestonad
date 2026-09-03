package no.nav.gjenlevende.bs.sak.barn

import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.pdl.PdlException
import no.nav.gjenlevende.bs.sak.pdl.PdlService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BarnService(
    private val pdlService: PdlService,
    private val barnRepository: BarnRepository,
    private val behandlingService: BehandlingService,
) {
    companion object {
        private const val CACHE_VALIDITY_HOURS = 4L
    }

    fun hentBarn(request: HentBarnRequest): List<HentBarnResponse> {
        val cachedBarn = barnRepository.findByBehandlingId(request.behandlingId)
        val avkutningsTidspunkt = LocalDateTime.now().minusHours(CACHE_VALIDITY_HOURS)

        val erCacheGyldig = cachedBarn.isNotEmpty() && cachedBarn.all { it.hentetTidspunkt.isAfter(avkutningsTidspunkt) }
        val erBehandlingRedigerbar = behandlingService.erBehandlingRedigerbar(request.behandlingId)

        return if (erCacheGyldig || !erBehandlingRedigerbar) {
            cachedBarn.map { barn ->
                HentBarnResponse(
                    id = barn.id,
                    personIdent = barn.personIdent,
                    navn = barn.navn,
                    fødselsdato = barn.fødselsdato,
                    hentetTidspunkt = barn.hentetTidspunkt,
                )
            }
        } else {
            hentOgLagreBarnFraPdl(request)
        }
    }

    private fun hentOgLagreBarnFraPdl(request: HentBarnRequest): List<HentBarnResponse> {
        val barnPersonIdenter = pdlService.hentBarnPersonidenter(request.personIdent)
        val hentetTidspunkt = LocalDateTime.now()

        val behandlingBarn =
            barnPersonIdenter.map { personIdent ->
                val person =
                    pdlService.hentPersonMedPersonIdent(personIdent)
                        ?: throw PdlException("Kunne ikke hente navn for barn med ident $personIdent")
                BehandlingBarn(
                    behandlingId = request.behandlingId,
                    personIdent = personIdent,
                    navn = listOfNotNull(person.navn.fornavn, person.navn.mellomnavn, person.navn.etternavn).joinToString(" "),
                    fødselsdato = person.foedselsdato,
                    hentetTidspunkt = hentetTidspunkt,
                )
            }

        barnRepository.deleteByBehandlingId(request.behandlingId)
        barnRepository.insertAll(behandlingBarn)

        return behandlingBarn.map { barn ->
            HentBarnResponse(
                id = barn.id,
                personIdent = barn.personIdent,
                navn = barn.navn,
                fødselsdato = barn.fødselsdato,
                hentetTidspunkt = barn.hentetTidspunkt,
            )
        }
    }
}
