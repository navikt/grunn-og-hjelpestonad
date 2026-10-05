package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.BehandlingsgrunnlagHenting
import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.BehandlingsgrunnlagHentingRepository
import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.Behandlingsgrunnlagskilde
import no.nav.grunn.og.hjelpestonad.pdl.PersonBehandlingsgrunnlag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PdlBehandlingsgrunnlagDbService(
    private val hentingRepository: BehandlingsgrunnlagHentingRepository,
    private val folkeregisterpersonstatusRepository: PdlFolkeregisterpersonstatusRepository,
    private val bostedsadresseRepository: PdlBostedsadresseRepository,
    private val statsborgerskapRepository: PdlStatsborgerskapRepository,
    private val oppholdRepository: PdlOppholdRepository,
    private val innflyttingTilNorgeRepository: PdlInnflyttingTilNorgeRepository,
    private val utflyttingFraNorgeRepository: PdlUtflyttingFraNorgeRepository,
) {
    fun hent(behandlingId: UUID): PdlBehandlingsgrunnlag? {
        val henting = hentingRepository.findByBehandlingIdAndKilde(behandlingId, Behandlingsgrunnlagskilde.PDL) ?: return null
        return PdlBehandlingsgrunnlag(
            hentetTidspunkt = henting.hentetTidspunkt,
            folkeregisterpersonstatus = folkeregisterpersonstatusRepository.findByBehandlingId(behandlingId),
            bostedsadresse = bostedsadresseRepository.findByBehandlingId(behandlingId),
            statsborgerskap = statsborgerskapRepository.findByBehandlingId(behandlingId),
            opphold = oppholdRepository.findByBehandlingId(behandlingId),
            innflyttingTilNorge = innflyttingTilNorgeRepository.findByBehandlingId(behandlingId),
            utflyttingFraNorge = utflyttingFraNorgeRepository.findByBehandlingId(behandlingId),
        )
    }

    @Transactional
    fun erstatt(
        behandlingId: UUID,
        person: PersonBehandlingsgrunnlag,
    ): PdlBehandlingsgrunnlag {
        slett(behandlingId)
        val henting = hentingRepository.insert(BehandlingsgrunnlagHenting(behandlingId = behandlingId, kilde = Behandlingsgrunnlagskilde.PDL))
        return PdlBehandlingsgrunnlag(
            hentetTidspunkt = henting.hentetTidspunkt,
            folkeregisterpersonstatus = folkeregisterpersonstatusRepository.insertAll(person.folkeregisterpersonstatus.map { it.tilPdlFolkeregisterpersonstatus(behandlingId) }),
            bostedsadresse = bostedsadresseRepository.insertAll(person.bostedsadresse.map { it.tilPdlBostedsadresse(behandlingId) }),
            statsborgerskap = statsborgerskapRepository.insertAll(person.statsborgerskap.map { it.tilPdlStatsborgerskap(behandlingId) }),
            opphold = oppholdRepository.insertAll(person.opphold.map { it.tilPdlOpphold(behandlingId) }),
            innflyttingTilNorge = innflyttingTilNorgeRepository.insertAll(person.innflyttingTilNorge.map { it.tilPdlInnflyttingTilNorge(behandlingId) }),
            utflyttingFraNorge = utflyttingFraNorgeRepository.insertAll(person.utflyttingFraNorge.map { it.tilPdlUtflyttingFraNorge(behandlingId) }),
        )
    }

    private fun slett(behandlingId: UUID) {
        hentingRepository.slett(behandlingId, Behandlingsgrunnlagskilde.PDL)
        folkeregisterpersonstatusRepository.slettForBehandling(behandlingId)
        bostedsadresseRepository.slettForBehandling(behandlingId)
        statsborgerskapRepository.slettForBehandling(behandlingId)
        oppholdRepository.slettForBehandling(behandlingId)
        innflyttingTilNorgeRepository.slettForBehandling(behandlingId)
        utflyttingFraNorgeRepository.slettForBehandling(behandlingId)
    }
}
