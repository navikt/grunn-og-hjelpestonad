package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.periode.Endringstyper
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseRepository
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService

/** Fellesdelen for vilkår: vurderingen og vilkårstypen. Livsløpet ligger i [PeriodeMedBegrunnelseService]. */
abstract class VilkårPeriodeService<VILKÅR_PERIODE : VilkårPeriode<VILKÅR_PERIODE>, R : VilkårPeriodeRequest>(
    repository: PeriodeMedBegrunnelseRepository<VILKÅR_PERIODE>,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : PeriodeMedBegrunnelseService<VILKÅR_PERIODE, R>(
        repository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
        Endringstyper(
            opprettet = EndringType.VILKÅR_VURDERING_OPPRETTET,
            oppdatert = EndringType.VILKÅR_VURDERING_OPPDATERT,
            slettet = EndringType.VILKÅR_VURDERING_SLETTET,
        ),
    ) {
    protected abstract val vilkårType: VilkårType

    override val periodebeskrivelse = "vilkårsvurdering"

    /**
     * Diagnose er helseopplysning etter GDPR artikkel 9 og skal aldri registreres i
     * endringshistorikk eller logg. Detaljer inneholder derfor bare vilkårstype, vurdering
     * og periode. Ikke utvid feltet med diagnosekode, diagnosebeskrivelse eller andre
     * opplysninger om saken.
     */
    override fun endringsdetaljer(periode: VILKÅR_PERIODE) = "$vilkårType: ${periode.vurdering}, Periode: ${formaterPeriode(periode)}"
}
