package no.nav.grunn.og.hjelpestonad.vilkår.institusjon

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårPeriodeService
import no.nav.grunn.og.hjelpestonad.vilkår.VilkårType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VilkårInstitusjonService(
    vilkårInstitusjonRepository: VilkårInstitusjonRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) : VilkårPeriodeService<VilkårInstitusjon, VilkårInstitusjonRequest>(
        vilkårInstitusjonRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
    ) {
    override val vilkårType = VilkårType.IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM

    override fun nyPeriode(
        behandlingId: UUID,
        request: VilkårInstitusjonRequest,
    ) = VilkårInstitusjon(
        behandlingId = behandlingId,
        oppholdstype = request.oppholdstype,
        unntakshjemmel = request.unntakshjemmel,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    override fun oppdatertPeriode(
        eksisterende: VilkårInstitusjon,
        request: VilkårInstitusjonRequest,
    ) = eksisterende.copy(
        oppholdstype = request.oppholdstype,
        unntakshjemmel = request.unntakshjemmel,
        vurdering = request.vurdering,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    override fun valider(request: VilkårInstitusjonRequest) {
        if (request.unntakshjemmel != null && request.oppholdstype == null) {
            throw Feil(
                melding = "Unntakshjemmel kan ikke settes uten at det er registrert et opphold.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
