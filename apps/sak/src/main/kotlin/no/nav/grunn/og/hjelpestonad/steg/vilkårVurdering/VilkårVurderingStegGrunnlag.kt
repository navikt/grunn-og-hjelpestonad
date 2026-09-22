package no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering

import no.nav.grunn.og.hjelpestonad.vilkår.diagnose.VilkårDiagnose
import no.nav.grunn.og.hjelpestonad.vilkår.institusjon.VilkårInstitusjon
import no.nav.grunn.og.hjelpestonad.vilkår.medlemskap.VilkårMedlemskap
import java.util.UUID

data class VilkårVurderingStegGrunnlag(
    val behandlingId: UUID,
    val medlemskap: List<VilkårMedlemskap>,
    val diagnoser: List<VilkårDiagnose>,
    val institusjon: List<VilkårInstitusjon>,
)
