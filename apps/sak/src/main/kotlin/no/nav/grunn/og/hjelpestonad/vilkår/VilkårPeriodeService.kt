package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

abstract class VilkårPeriodeService<T : VilkårPeriode, R : VilkårPeriodeRequest>(
    private val repository: VilkårPeriodeRepository<T>,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    protected abstract val vilkårType: VilkårType

    protected abstract fun nyPeriode(
        behandlingId: UUID,
        request: R,
    ): T

    protected abstract fun oppdatertPeriode(
        eksisterende: T,
        request: R,
    ): T

    protected open fun perioderSomIkkeKanOverlappe(
        lagredePerioder: List<T>,
        request: R,
    ): List<T> = lagredePerioder

    protected open fun valider(request: R) = Unit

    fun hentPerioder(behandlingId: UUID): List<T> = repository.findByBehandlingId(behandlingId)

    fun hentPeriodePåBehandling(
        behandlingId: UUID,
        periodeId: UUID,
    ): T =
        repository
            .findByIdOrNull(periodeId)
            ?.takeIf { it.behandlingId == behandlingId }
            ?: throw Feil(
                melding = "Fant ikke vilkårsvurdering med id=$periodeId på behandling=$behandlingId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

    @Transactional
    fun lagrePeriode(
        behandlingId: UUID,
        request: R,
    ): T {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        valider(request)
        validerIngenOverlapp(behandlingId, request)

        val periodeId = request.id
        return if (periodeId == null) {
            repository.insert(nyPeriode(behandlingId, request)).also {
                registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPRETTET, it.vurdering)
            }
        } else {
            repository.update(oppdatertPeriode(hentPeriodePåBehandling(behandlingId, periodeId), request)).also {
                registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_OPPDATERT, it.vurdering)
            }
        }
    }

    @Transactional
    fun slettPeriode(
        behandlingId: UUID,
        periodeId: UUID,
    ) {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val periode = hentPeriodePåBehandling(behandlingId, periodeId)

        repository.deleteById(periode.id)

        registrerEndring(behandlingId, EndringType.VILKÅR_VURDERING_SLETTET, periode.vurdering)
    }

    private fun validerIngenOverlapp(
        behandlingId: UUID,
        request: R,
    ) = perioderSomIkkeKanOverlappe(repository.findByBehandlingId(behandlingId), request)
        .filterNot { it.id == request.id }
        .validerIngenOverlappMed(request)

    /**
     * Diagnose er helseopplysning etter GDPR artikkel 9 og skal aldri registreres i
     * endringshistorikk eller logg. Detaljer inneholder derfor bare vilkårstype og vurdering.
     * Ikke utvid feltet med diagnosekode, diagnosebeskrivelse eller andre opplysninger om
     * saken.
     */
    private fun registrerEndring(
        behandlingId: UUID,
        endringType: EndringType,
        vurdering: Vurdering,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = endringType,
        detaljer = "$vilkårType: $vurdering",
    )
}
