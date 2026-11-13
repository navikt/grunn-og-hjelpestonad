package no.nav.grunn.og.hjelpestonad.felles.periode

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** Endringstypene servicen skriver i endringshistorikken. Vilkårene bruker VILKÅR_VURDERING_*. */
data class Endringstyper(
    val opprettet: EndringType,
    val oppdatert: EndringType,
    val slettet: EndringType,
)

abstract class PeriodeMedBegrunnelseService<P : PeriodeMedBegrunnelse<P>, R : PeriodeMedBegrunnelseRequest>(
    private val repository: PeriodeMedBegrunnelseRepository<P>,
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val endringstyper: Endringstyper,
) {
    protected abstract val periodebeskrivelse: String

    protected abstract fun nyPeriode(
        behandlingId: UUID,
        request: R,
    ): P

    protected abstract fun P.oppdaterMed(
        request: R,
    ): P

    protected open fun valider(
        request: R,
        behandlingId: UUID,
        eksisterendePeriode: P?,
    ) = Unit

    protected open fun validerTidslinje(perioder: List<P>) = Unit

    /** Om perioden skal kopieres til en ny behandling. Perioder uten vurdering, som trygdetid, kopieres alltid. */
    protected open fun skalKopieres(periode: P): Boolean = true

    open fun hentPerioder(behandlingId: UUID): List<P> = repository.findByBehandlingId(behandlingId)

    @Transactional
    open fun kopierPerioder(
        fraBehandlingId: UUID,
        tilBehandlingId: UUID,
    ): List<P> {
        val kopier =
            repository
                .findByBehandlingId(fraBehandlingId)
                .filter { skalKopieres(it) }
                .map { it.kopierTilBehandling(tilBehandlingId) }
        repository.insertAll(kopier)
        return kopier
    }

    open fun hentPeriodePåBehandling(
        behandlingId: UUID,
        periodeId: UUID,
    ): P =
        repository
            .findByIdOrNull(periodeId)
            ?.takeIf { it.behandlingId == behandlingId }
            ?: throw Feil(
                melding = "Fant ikke $periodebeskrivelse med id=$periodeId på behandling=$behandlingId",
                httpStatus = HttpStatus.NOT_FOUND,
            )

    @Transactional
    open fun lagrePeriode(
        behandlingId: UUID,
        request: R,
    ): P {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)

        validerBegrunnelse(request.begrunnelse)
        val eksisterendePeriode = request.id?.let { hentPeriodePåBehandling(behandlingId, it) }
        valider(request = request, behandlingId = behandlingId, eksisterendePeriode = eksisterendePeriode)
        val periode = eksisterendePeriode?.oppdaterMed(request) ?: nyPeriode(behandlingId, request)

        val perioder = repository.findByBehandlingId(behandlingId).medNyPeriode(periode)
        validerTidslinje(perioder)

        repository.deleteByBehandlingId(behandlingId)
        repository.insertAll(perioder)

        val endringType = if (eksisterendePeriode == null) endringstyper.opprettet else endringstyper.oppdatert
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

        registrerEndring(behandlingId, endringstyper.slettet, periode)
    }

    protected abstract fun endringsdetalj(periode: P): String

    private fun registrerEndring(
        behandlingId: UUID,
        endringType: EndringType,
        periode: P,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = endringType,
        detaljer =
            historikktekst(
                endringsdetalj = endringsdetalj(periode = periode),
                periode = periode,
            ),
    )
}
