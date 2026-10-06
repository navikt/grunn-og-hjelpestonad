package no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.grunn.og.hjelpestonad.SpringContextTest
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingRepository
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakPersonRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakRepository
import no.nav.grunn.og.hjelpestonad.fagsak.FagsakService
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.pdl.Bostedsadresse
import no.nav.grunn.og.hjelpestonad.pdl.Folkeregistermetadata
import no.nav.grunn.og.hjelpestonad.pdl.Folkeregisterpersonstatus
import no.nav.grunn.og.hjelpestonad.pdl.InnflyttingTilNorge
import no.nav.grunn.og.hjelpestonad.pdl.Metadata
import no.nav.grunn.og.hjelpestonad.pdl.Opphold
import no.nav.grunn.og.hjelpestonad.pdl.PdlException
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.pdl.PersonBehandlingsgrunnlag
import no.nav.grunn.og.hjelpestonad.pdl.Statsborgerskap
import no.nav.grunn.og.hjelpestonad.pdl.UkjentBosted
import no.nav.grunn.og.hjelpestonad.pdl.UtenlandskAdresse
import no.nav.grunn.og.hjelpestonad.pdl.UtflyttingFraNorge
import no.nav.grunn.og.hjelpestonad.pdl.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime

class PdlBehandlingsgrunnlagServiceTest(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakService: FagsakService,
    private val dbService: PdlBehandlingsgrunnlagDbService,
    private val transactionTemplate: TransactionTemplate,
) : SpringContextTest() {
    private val pdlService = mockk<PdlService>()
    private val ident = (10_000_000_000L..99_999_999_999L).random().toString()

    private val service =
        PdlBehandlingsgrunnlagService(
            pdlService = pdlService,
            behandlingService = behandlingService,
            ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true),
            fagsakService = fagsakService,
            pdlBehandlingsgrunnlagDbService = dbService,
        )

    @Test
    fun `lagrer personopplysningene slik PDL leverte dem`() {
        val behandling = opprettBehandling()
        every { pdlService.hentBehandlingsgrunnlag(ident) } returns person

        service.innhentBehandlingsgrunnlagFraPdl(behandling)

        val grunnlag = service.hent(behandling.id)!!
        assertThat(grunnlag.folkeregisterpersonstatus.single().status).isEqualTo("bosatt")
        assertThat(grunnlag.folkeregisterpersonstatus.single().metadata)
            .isEqualTo(PdlMetadata(historisk = false, master = "FREG", gyldighetstidspunkt = LocalDateTime.of(2020, 1, 1, 0, 0)))
        assertThat(grunnlag.bostedsadresse.map { it.adressetype to (it.kommunenummer ?: it.bostedskommune ?: it.landkode) })
            .containsExactlyInAnyOrder(
                PdlAdressetype.VEGADRESSE to "0301",
                PdlAdressetype.UTENLANDSK_ADRESSE to "SWE",
                PdlAdressetype.UKJENT_BOSTED to "4601",
            )
        assertThat(grunnlag.statsborgerskap.map { it.land }).containsExactlyInAnyOrder("NOR", "SWE")
        assertThat(grunnlag.opphold.single().type).isEqualTo("MIDLERTIDIG")
        assertThat(grunnlag.innflyttingTilNorge.single().fraflyttingsstedIUtlandet).isEqualTo("Göteborg")
        assertThat(grunnlag.utflyttingFraNorge.single().utflyttingsdato).isEqualTo(LocalDate.of(2015, 1, 1))
    }

    @Test
    fun `ny innhenting erstatter det som er lagret for behandlingen, men ikke for andre behandlinger`() {
        val behandling = opprettBehandling()
        val annenBehandling = opprettBehandling()
        every { pdlService.hentBehandlingsgrunnlag(ident) } returns person
        service.innhentBehandlingsgrunnlagFraPdl(behandling)
        service.innhentBehandlingsgrunnlagFraPdl(annenBehandling)
        val førsteHenting = service.hent(behandling.id)!!.hentetTidspunkt

        every { pdlService.hentBehandlingsgrunnlag(ident) } returns
            PersonBehandlingsgrunnlag(statsborgerskap = listOf(statsborgerskap("DNK")))
        val nyttGrunnlag = service.innhentBehandlingsgrunnlagFraPdl(behandling.id)

        assertThat(nyttGrunnlag.hentetTidspunkt).isAfterOrEqualTo(førsteHenting)
        assertThat(nyttGrunnlag.statsborgerskap.map { it.land }).containsExactly("DNK")
        assertThat(nyttGrunnlag.folkeregisterpersonstatus).isEmpty()
        assertThat(nyttGrunnlag.bostedsadresse).isEmpty()
        assertThat(service.hent(behandling.id)).isEqualTo(nyttGrunnlag)
        assertThat(service.hent(annenBehandling.id)!!.statsborgerskap.map { it.land }).containsExactlyInAnyOrder("NOR", "SWE")
    }

    @Test
    fun `tomt svar fra PDL skilles fra ikke hentet`() {
        val behandling = opprettBehandling()
        assertThat(service.hent(behandling.id)).isNull()

        every { pdlService.hentBehandlingsgrunnlag(ident) } returns PersonBehandlingsgrunnlag()
        service.innhentBehandlingsgrunnlagFraPdl(behandling)

        val grunnlag = service.hent(behandling.id)!!
        assertThat(grunnlag.folkeregisterpersonstatus).isEmpty()
        assertThat(grunnlag.statsborgerskap).isEmpty()
    }

    @Test
    fun `feil fra PDL ved opprettelse kastes videre og lagrer ingenting`() {
        val behandling = opprettBehandling()
        every { pdlService.hentBehandlingsgrunnlag(ident) } throws PdlException("PDL er nede")

        assertThatThrownBy { service.innhentBehandlingsgrunnlagFraPdl(behandling) }.isInstanceOf(PdlException::class.java)

        assertThat(service.hent(behandling.id)).isNull()
    }

    @Test
    fun `innhenting i transaksjonen som oppretter behandlingen lagrer begge`() {
        every { pdlService.hentBehandlingsgrunnlag(ident) } returns person

        val behandling =
            transactionTemplate.execute {
                opprettBehandling().also { service.innhentBehandlingsgrunnlagFraPdl(it) }
            }!!

        assertThat(behandlingRepository.existsById(behandling.id)).isTrue
        assertThat(service.hent(behandling.id)!!.statsborgerskap).hasSize(2)
    }

    @Test
    fun `feil fra PDL i transaksjonen som oppretter behandlingen ruller tilbake behandlingen`() {
        every { pdlService.hentBehandlingsgrunnlag(ident) } throws PdlException("PDL er nede")
        val behandling = Behandling(fagsakId = opprettFagsak().id, status = BehandlingStatus.UTREDES, resultat = BehandlingResultat.IKKE_SATT)

        assertThatThrownBy {
            transactionTemplate.executeWithoutResult {
                behandlingRepository.insert(behandling)
                service.innhentBehandlingsgrunnlagFraPdl(behandling)
            }
        }.isInstanceOf(PdlException::class.java)

        assertThat(behandlingRepository.existsById(behandling.id)).isFalse
    }

    @Test
    fun `feil fra PDL ved ny innhenting beholder det som er lagret`() {
        val behandling = opprettBehandling()
        every { pdlService.hentBehandlingsgrunnlag(ident) } returns person
        service.innhentBehandlingsgrunnlagFraPdl(behandling)

        every { pdlService.hentBehandlingsgrunnlag(ident) } throws PdlException("PDL er nede")

        assertThatThrownBy { service.innhentBehandlingsgrunnlagFraPdl(behandling.id) }.isInstanceOf(PdlException::class.java)
        assertThat(service.hent(behandling.id)!!.statsborgerskap).hasSize(2)
    }

    @Test
    fun `kan ikke innhente på nytt når behandlingen ikke kan redigeres`() {
        val behandlingId = opprettBehandling(BehandlingStatus.FATTER_VEDTAK).id

        assertThatThrownBy { service.innhentBehandlingsgrunnlagFraPdl(behandlingId) }.isInstanceOf(Feil::class.java)
    }

    private val person =
        PersonBehandlingsgrunnlag(
            folkeregisterpersonstatus =
                listOf(
                    Folkeregisterpersonstatus(
                        status = "bosatt",
                        forenkletStatus = "bosattEtterFolkeregisterloven",
                        metadata = Metadata(historisk = false, master = "FREG"),
                        folkeregistermetadata = Folkeregistermetadata(gyldighetstidspunkt = LocalDateTime.of(2020, 1, 1, 0, 0)),
                    ),
                ),
            bostedsadresse =
                listOf(
                    Bostedsadresse(vegadresse = Vegadresse("0301"), metadata = Metadata(historisk = false, master = "FREG")),
                    Bostedsadresse(utenlandskAdresse = UtenlandskAdresse("SWE"), metadata = Metadata(historisk = true, master = "FREG")),
                    Bostedsadresse(ukjentBosted = UkjentBosted("4601"), metadata = Metadata(historisk = true, master = "FREG")),
                ),
            statsborgerskap = listOf(statsborgerskap("NOR"), statsborgerskap("SWE")),
            opphold =
                listOf(
                    Opphold(type = "MIDLERTIDIG", oppholdFra = LocalDate.of(2014, 1, 1), metadata = Metadata(historisk = true, master = "FREG")),
                ),
            innflyttingTilNorge =
                listOf(
                    InnflyttingTilNorge(
                        fraflyttingsland = "SWE",
                        fraflyttingsstedIUtlandet = "Göteborg",
                        metadata = Metadata(historisk = false, master = "FREG"),
                    ),
                ),
            utflyttingFraNorge =
                listOf(
                    UtflyttingFraNorge(
                        tilflyttingsland = "SWE",
                        utflyttingsdato = LocalDate.of(2015, 1, 1),
                        metadata = Metadata(historisk = false, master = "FREG"),
                    ),
                ),
        )

    private fun statsborgerskap(land: String) = Statsborgerskap(land = land, gyldigFraOgMed = LocalDate.of(1990, 1, 15), metadata = Metadata(historisk = false, master = "FREG"))

    private fun opprettBehandling(status: BehandlingStatus = BehandlingStatus.UTREDES): Behandling = behandlingRepository.insert(Behandling(fagsakId = opprettFagsak().id, status = status, resultat = BehandlingResultat.IKKE_SATT))

    private fun opprettFagsak(): Fagsak {
        val fagsakPerson =
            fagsakPersonRepository.findByIdent(setOf(ident))
                ?: fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(ident))))
        return fagsakRepository.findByFagsakPersonIdAndStønadstype(fagsakPerson.id, StønadType.BARNETILSYN)
            ?: fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = StønadType.BARNETILSYN))
    }
}
