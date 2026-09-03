package no.nav.gjenlevende.bs.sak.brev.domain

data class BrevRequest(
    val brevmal: BrevmalDto,
    val fritekstbolker: List<TekstbolkDto>,
)

data class BrevmalDto(
    val tittel: String,
    val informasjonOmBruker: InformasjonOmBrukerDto,
    val fastTekstAvslutning: List<TekstbolkDto>,
)

data class InformasjonOmBrukerDto(
    val navn: String,
    val fnr: String,
)

data class TekstbolkDto(
    val underoverskrift: String?,
    val innhold: String,
)
