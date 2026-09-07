package no.nav.grunn.og.hjelpestonad.brev

import no.nav.grunn.og.hjelpestonad.brev.domain.BrevRequest
import no.nav.grunn.og.hjelpestonad.felles.sporbar.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("brev")
data class Brev(
    @Id
    val behandlingId: UUID,
    val brevJson: BrevRequest,
    val brevPdf: ByteArray? = null,
    val saksbehandler: String? = null,
    val saksbehandlerEnhet: String? = null,
    val beslutter: String? = null,
    val beslutterEnhetnavn: String? = null,
    val beslutterEnhetnummer: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
