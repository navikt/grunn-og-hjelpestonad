package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.util.UUID

abstract class VilkårPeriodeService<VILKÅR_PERIODE : VilkårPeriode<VILKÅR_PERIODE>, R : VilkårPeriodeRequest>(
    private val repository: VilkårPeriodeRepository<VILKÅR_PERIODE>,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    protected abstract val vilkårType: VilkårType

    protected abstract fun nyPeriode(
        behandlingId: UUID,
        request: R,
    ): VILKÅR_PERIODE

    protected abstract fun oppdatertPeriode(
        eksisterende: VILKÅR_PERIODE,
        request: R,
    ): VILKÅR_PERIODE

    protected open fun perioderSomIkkeKanOverlappe(
        lagredePerioder: List<VILKÅR_PERIODE>,
        request: R,
    ): List<VILKÅR_PERIODE> = lagredePerioder

    protected open fun valider(request: R) = Unit

    open fun hentPerioder(behandlingId: UUID): List<VILKÅR_PERIODE> = repository.findByBehandlingId(behandlingId)

    open fun hentPeriodePåBehandling(
        behandlingId: UUID,
        periodeId: UUID,
    ): VILKÅR_PERIODE =
        repository
            .findByIdOrNull(periodeId)
            ?.takeIf { it.behandlingId == behandlingId }
            ?: throw Feil(
                melding = "Fant ikke vilkårsvurdering med id=$periodeId på behandling=$behandlingId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

    @Transactional
    open fun lagrePeriode(
        behandlingId: UUID,
        request: R,
    ): VILKÅR_PERIODE {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        valider(request)
        forkortOverlappendePerioder(behandlingId, request)

        val periodeId = request.id
        return if (periodeId == null) {
            repository.insert(nyPeriode(behandlingId, request)).also {
                registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPRETTET, it)
            }
        } else {
            repository.update(oppdatertPeriode(hentPeriodePåBehandling(behandlingId, periodeId), request)).also {
                registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPDATERT, it)
            }
        }
    }

    @Transactional
    open fun slettPeriode(
        behandlingId: UUID,
        periodeId: UUID,
    ) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val periode = hentPeriodePåBehandling(behandlingId, periodeId)

        repository.deleteById(periode.id)

        registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_SLETTET, periode)
    }

    /**
     * Den nye eller endrede perioden vinner ved overlapp. Lagrede perioder som overlapper blir
     * forkortet, splittet i to hvis den nye perioden ligger midt inni dem, eller slettet hvis de
     * blir dekket i sin helhet.
     */
    private fun forkortOverlappendePerioder(
        behandlingId: UUID,
        request: R,
    ) {
        val lagredePerioder =
            perioderSomIkkeKanOverlappe(repository.findByBehandlingId(behandlingId), request)
                .filterNot { it.id == request.id }
        val gjenværendeTidsrom = lagredePerioder.forkortetAv(request).groupBy { it.verdi.id }

        lagredePerioder.forEach { lagret ->
            val tidsrom = gjenværendeTidsrom[lagret.id].orEmpty()

            if (
                tidsrom.isEmpty() ||
                tidsrom.any { it.fom != lagret.fraOgMedDato } ||
                tidsrom.any { it.tom != lagret.tilOgMedDato }
            ) {
                repository.deleteById(lagret.id)
                tidsrom.forEach {
                    repository.insert(
                        it.verdi.kopierMedTidsrom(
                            id = UUID.randomUUID(),
                            fraOgMedDato = it.fom,
                            tilOgMedDato = it.tom,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Diagnose er helseopplysning etter GDPR artikkel 9 og skal aldri registreres i
     * endringshistorikk eller logg. Detaljer inneholder derfor bare vilkårstype, vurdering
     * og periode. Ikke utvid feltet med diagnosekode, diagnosebeskrivelse eller andre
     * opplysninger om saken.
     */
    private fun registrerEndring(
        behandlingId: UUID,
        endringType: EndringType,
        periode: VilkårPeriode<*>,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = endringType,
        detaljer = "$vilkårType: ${periode.vurdering}, Periode: ${formaterPeriode(periode)}",
    )

    private fun formaterPeriode(periode: Periodisert): String {
        if (periode.fraOgMedDato == null && periode.tilOgMedDato == null) {
            return "Hele behandlingsperioden"
        }

        val fra = periode.fraOgMedDato?.format(DATOFORMAT) ?: "Fra fødsel"
        val til = periode.tilOgMedDato?.format(DATOFORMAT) ?: "løpende"
        return "$fra – $til"
    }

    private companion object {
        val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
