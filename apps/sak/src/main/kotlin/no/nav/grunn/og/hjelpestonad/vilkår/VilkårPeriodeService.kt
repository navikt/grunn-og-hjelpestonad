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

    protected abstract fun VILKÅR_PERIODE.oppdaterMed(
        request: R,
    ): VILKÅR_PERIODE

    protected open fun valider(request: R) = Unit

    protected open fun validerTidslinje(perioder: List<VILKÅR_PERIODE>) = Unit

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

        val eksisterendePeriode = request.id?.let { hentPeriodePåBehandling(behandlingId, it) }
        valider(request)
        val periode = eksisterendePeriode?.oppdaterMed(request) ?: nyPeriode(behandlingId, request)

        val perioder = repository.findByBehandlingId(behandlingId).medNyPeriode(periode)
        validerTidslinje(perioder)

        repository.deleteByBehandlingId(behandlingId)
        repository.insertAll(perioder)

        val endringType = if (eksisterendePeriode == null) EndringType.VILKÅR_VURDERING_OPPRETTET else EndringType.VILKÅR_VURDERING_OPPDATERT
        registrerEndring(behandlingId, endringType, periode)
        return periode
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
