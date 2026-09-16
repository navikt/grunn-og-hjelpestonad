package no.nav.grunn.og.hjelpestonad.vilkår

import java.time.LocalDate
import java.util.UUID

data class DiagnoseResponse(
    val id: UUID,
    val vilkårVurderingId: UUID,
    val diagnose: String,
    val yrkesskade: Boolean,
    val yrkesskadeDato: LocalDate?,
)

data class DiagnoseRequest(
    val id: UUID? = null,
    val diagnose: String,
    val yrkesskade: Boolean = false,
    val yrkesskadeDato: LocalDate? = null,
)

fun VilkårDiagnose.tilResponse() =
    DiagnoseResponse(
        id = this.id,
        vilkårVurderingId = this.vilkårVurderingId,
        diagnose = this.diagnose,
        yrkesskade = this.yrkesskade,
        yrkesskadeDato = this.yrkesskadeDato,
    )
