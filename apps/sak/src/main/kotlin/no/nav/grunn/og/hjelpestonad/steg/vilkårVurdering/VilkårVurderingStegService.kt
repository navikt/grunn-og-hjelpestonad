package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.Periodisert
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRett
import no.nav.grunn.og.hjelpestonad.vilkår.rett.PeriodeMedRettRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class VilkårVurderingStegService(
    private val vilkårMedlemskapService: VilkårMedlemskapService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val vilkårInstitusjonService: VilkårInstitusjonService,
    private val periodeMedRettRepository: PeriodeMedRettRepository,
    private val behandlingService: BehandlingService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val endringshistorikkService: EndringshistorikkService,
) {
    @Transactional
    fun fullførSteg(behandlingId: UUID): List<PeriodeMedRett> {
        behandlingService.validerBehandlingErRedigerbar(behandlingId)
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)

        val perioderMedRett = PeriodeMedRettUtleder.utled(hentVilkårVurderingStegGrunnlag(behandlingId))

        periodeMedRettRepository.deleteByBehandlingId(behandlingId)
        val lagrede = periodeMedRettRepository.insertAll(perioderMedRett)

        registrerEndringshistorikk(behandlingId, lagrede)
        return lagrede
    }

    private fun hentVilkårVurderingStegGrunnlag(behandlingId: UUID) =
        VilkårVurderingStegGrunnlag(
            behandlingId = behandlingId,
            medlemskap = vilkårMedlemskapService.hentPerioder(behandlingId),
            diagnoser = vilkårDiagnoseService.hentPerioder(behandlingId),
            institusjon = vilkårInstitusjonService.hentPerioder(behandlingId),
        )

    private fun registrerEndringshistorikk(
        behandlingId: UUID,
        perioderMedRett: List<PeriodeMedRett>,
    ) = endringshistorikkService.registrerEndring(
        behandlingId = behandlingId,
        endringType = EndringType.VILKÅR_VURDERING_FULLFØRT,
        detaljer =
            if (perioderMedRett.isEmpty()) {
                "Ingen perioder med oppfylte vilkår"
            } else {
                "${perioderMedRett.size} perioder med oppfylte vilkår: " +
                    perioderMedRett.joinToString(", ") { formaterPeriode(it) }
            },
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
