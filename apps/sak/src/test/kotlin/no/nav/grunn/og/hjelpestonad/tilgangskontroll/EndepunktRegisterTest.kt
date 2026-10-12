package no.nav.grunn.og.hjelpestonad.tilgangskontroll

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Sikrer at hvert REST-endepunkt er plassert: enten sjekker det tilgang til person via TilgangService,
 * eller det er bevisst unntatt. Et nytt endepunkt får testen til å feile til det er klassifisert.
 * Se ADR-0009.
 */
class EndepunktRegisterTest {
    @Test
    fun `alle endepunkter er klassifisert`() {
        val funnet = alleHandlere()

        val ikkeKlassifisert = funnet - (MED_PERSONSJEKK + UTEN_PERSONSJEKK)
        val finnesIkke = (MED_PERSONSJEKK + UTEN_PERSONSJEKK) - funnet

        assertTrue(
            ikkeKlassifisert.isEmpty(),
            "Nye endepunkter må kalle tilgangService.validerTilgangTil…() og legges i MED_PERSONSJEKK, " +
                "eller legges i UTEN_PERSONSJEKK med begrunnelse (se ADR-0009): $ikkeKlassifisert",
        )
        assertTrue(finnesIkke.isEmpty(), "Endepunkter i listene som ikke finnes lenger: $finnesIkke")
    }

    @Test
    fun `ingen endepunkter står i begge listene`() {
        assertEquals(emptySet<String>(), MED_PERSONSJEKK intersect UTEN_PERSONSJEKK)
    }

    private fun alleHandlere(): Set<String> {
        // Skanneren tar hensyn til @Profile. "dev" tar med controllere som bare finnes i dev, som UnleashController.
        val miljø = StandardEnvironment().apply { setActiveProfiles("dev") }
        val skanner = ClassPathScanningCandidateComponentProvider(false, miljø)
        skanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))

        return skanner
            .findCandidateComponents("no.nav.grunn.og.hjelpestonad")
            .map { Class.forName(it.beanClassName) }
            .flatMap { klasse ->
                klasse.declaredMethods
                    .filter { AnnotatedElementUtils.hasAnnotation(it, RequestMapping::class.java) }
                    .map { "${klasse.simpleName}#${it.name}" }
            }.toSet()
    }

    companion object {
        private val MED_PERSONSJEKK =
            setOf(
                "BehandlingController#opprettBehandling",
                "BehandlingController#henleggBehandling",
                "BehandlingController#hentBehandlinger",
                "BehandlingController#hentBehandling",
                "BeslutterController#sendTilBeslutter",
                "BeslutterController#angreSendTilBeslutter",
                "BeslutterController#besluttVedtak",
                "BeslutterController#hentTotrinnskontrollStatus",
                "BrevController#lagBrevTask",
                "BrevController#mellomlagreBrev",
                "BrevController#hentMellomlagretBrev",
                "BrevmottakerController#hentBrevmottakere",
                "BrevmottakerController#oppdaterBrevmottakere",
                "EndringshistorikkController#hentEndringshistorikk",
                "FagsakController#opprettFagsak",
                "FagsakController#hentFagsak",
                "SøkController#søkPerson",
                "SøkController#søkPersonMedFagsakPersonId",
                "PdlFaktagrunnlagController#hentMedlemskapFaktagrunnlag",
                "PdlFaktagrunnlagController#innhentFaktagrunnlagFraPdl",
                "InfotrygdController#hentPerioderForPerson",
                "IverksettVedtaksbrevController#lagIverksettVedtaksbrevTask",
                "IverksettVedtaksbrevController#distribuerVedtaksbrev",
                "SimuleringController#simulerForBehandling",
                "SimuleringController#hentSimuleringsresultat",
                "OppgaveController#hentAnsvarligSaksbehandler",
                "OppgaveController#fordelOppgave",
                "PdlController#hentPerson",
                "SafController#hentJournalPostForBrukerId",
                "SafController#hentDokumentSomPdf",
                "VilkårVurderingStegController#fullførSteg",
                "VedtakController#hentVedtak",
                "VedtakController#lagreVedtak",
                "VedtakController#beregn",
                "VedtakController#hentHistoriskVedtakForBehanling",
                "VilkårDiagnoseController#hentDiagnosePerioder",
                "VilkårDiagnoseController#lagreDiagnosePeriode",
                "VilkårDiagnoseController#slettDiagnosePeriode",
                "VilkårInstitusjonController#hentInstitusjonPerioder",
                "VilkårInstitusjonController#lagreInstitusjonPeriode",
                "VilkårInstitusjonController#slettInstitusjonPeriode",
                "VilkårMedlemskapController#hentMedlemskapPerioder",
                "VilkårMedlemskapController#lagreMedlemskapPeriode",
                "VilkårMedlemskapController#slettMedlemskapPeriode",
                "PeriodeMedRettController#hentPerioderMedRett",
                "ÅrsakBehandlingController#hentÅrsakBehandling",
                "ÅrsakBehandlingController#lagreÅrsakBehandling",
            )

        // Tar ikke imot noe som peker på en person.
        private val UTEN_PERSONSJEKK =
            setOf(
                "DiagnosekodeController#søkDiagnosekoder",
                "UnleashController#hentFeatureToggles",
                "TestDbController#hentTestData",
            )
    }
}
