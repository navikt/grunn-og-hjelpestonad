package no.nav.grunn.og.hjelpestonad.trygdetid

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.periode.Endringstyper
import no.nav.grunn.og.hjelpestonad.felles.periode.PeriodeMedBegrunnelseService
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TrygdetidService(
    trygdetidRepository: TrygdetidRepository,
    behandlingService: BehandlingService,
    endringshistorikkService: EndringshistorikkService,
    ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    tilkjentYtelseService: TilkjentYtelseService,
) : PeriodeMedBegrunnelseService<Trygdetid, TrygdetidRequest>(
        trygdetidRepository,
        behandlingService,
        endringshistorikkService,
        ansvarligSaksbehandlerService,
        Endringstyper(
            opprettet = EndringType.TRYGDETID_OPPRETTET,
            oppdatert = EndringType.TRYGDETID_OPPDATERT,
            slettet = EndringType.TRYGDETID_SLETTET,
        ),
        tilkjentYtelseService,
    ) {
    override val periodebeskrivelse = "trygdetid"

    override val steg = BehandlingSteg.SATS_OG_TRYGDETID

    override fun valider(
        request: TrygdetidRequest,
        behandlingId: UUID,
        eksisterendePeriode: Trygdetid?,
    ) = TrygdetidRegler.valider(request)

    override fun nyPeriode(
        behandlingId: UUID,
        request: TrygdetidRequest,
    ) = Trygdetid(
        behandlingId = behandlingId,
        teller = request.teller,
        kilde = request.kilde,
        begrunnelse = request.begrunnelse,
        fraOgMedDato = request.fraOgMedDato,
        tilOgMedDato = request.tilOgMedDato,
    )

    override fun Trygdetid.oppdaterMed(request: TrygdetidRequest) =
        copy(
            teller = request.teller,
            kilde = request.kilde,
            begrunnelse = request.begrunnelse,
            fraOgMedDato = request.fraOgMedDato,
            tilOgMedDato = request.tilOgMedDato,
        )

    override fun endringsdetalj(periode: Trygdetid) = "TRYGDETID"
}
