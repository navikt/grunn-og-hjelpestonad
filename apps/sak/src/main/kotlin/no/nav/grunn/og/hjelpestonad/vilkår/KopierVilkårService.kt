package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnoseService
import no.nav.grunn.og.hjelpestonad.vilkår.ekstrautgifter.VilkårEkstrautgifterService
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskapService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KopierVilkårService(
    private val vilkårMedlemskapService: VilkårMedlemskapService,
    private val vilkårDiagnoseService: VilkårDiagnoseService,
    private val vilkårEkstrautgifterService: VilkårEkstrautgifterService,
) {
    @Transactional
    fun kopierFraForrigeBehandling(
        forrigeBehandlingId: UUID,
        behandlingId: UUID,
    ) {
        listOf(vilkårMedlemskapService, vilkårDiagnoseService, vilkårEkstrautgifterService).forEach {
            it.kopierPerioder(fraBehandlingId = forrigeBehandlingId, tilBehandlingId = behandlingId)
        }
    }
}
