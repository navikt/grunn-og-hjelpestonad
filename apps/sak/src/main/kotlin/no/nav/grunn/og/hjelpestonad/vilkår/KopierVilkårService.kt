package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjonService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * En revurdering starter med vilkårene fra forrige iverksatte behandling, som i EF.
 * Perioder med rett kopieres ikke. De utledes når vilkårssteget fullføres.
 */
@Service
class KopierVilkårService(
    private val vilkårMedlemskapService: VilkårMedlemskapService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val vilkårInstitusjonService: VilkårInstitusjonService,
) {
    @Transactional
    fun kopierFraForrigeBehandling(
        forrigeBehandlingId: UUID,
        behandlingId: UUID,
    ) {
        val antallKopiert =
            listOf(vilkårMedlemskapService, vilkårDiagnoseService, vilkårInstitusjonService)
                .sumOf { it.kopierPerioder(fraBehandlingId = forrigeBehandlingId, tilBehandlingId = behandlingId).size }

        if (antallKopiert == 0) {
            throw Feil("Forrige behandling $forrigeBehandlingId har ingen vilkår som kan kopieres")
        }
    }
}
