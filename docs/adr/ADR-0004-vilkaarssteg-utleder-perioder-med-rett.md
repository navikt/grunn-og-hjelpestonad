# ADR-0004: Vilkårssteget fullføres i backend og utleder perioder med rett

**Dato:** 2026-09-22
**Status:** Godkjent
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad
**Bygger på:** [ADR-0003](ADR-0003-vilkaarsmodell-for-grunnstoenad.md)

## Kontekst

ADR-0003 innførte tre periodiserte vilkår for grunnstønad, men utsatte bevisst
aggregeringen av «alle vilkår oppfylt». Konsekvensen er at `vurdering` lagres per
rad uten at noen leser radene samlet.

I dag ligger hele stegbegrepet i frontend. `routes/behandling/vilkår.tsx` sjekker
selv `vilkårStatus.alleVilkårErUtfylt` — som bare betyr «minst én periode per
vilkår» — og navigerer videre uten å kontakte backend. Koden bærer en TODO om at
dette bør flyttes:

```tsx
// TODO: Øsker å nuke denne og flytte all steglogikk backend.
const { navigerTilNeste } = useStegNavigering(STEG_PATH);
```

Et søk etter «steg» i `apps/sak/src` gir null treff. Backend vet altså ikke at
vilkårssteget finnes, og har ingen mening om når det er ferdig.

Det gir tre problemer:

- **Regler uten håndheving.** En klient kan hoppe rett til neste steg. Kravet om
  fullstendig vilkårsvurdering finnes bare som JavaScript.
- **Ingen lagret konklusjon.** Beregning og brev må selv tolke vilkårsradene, og
  vil gjøre det hver for seg.
- **Utledningen er ikke triviell.** Flere samtidige diagnoser, hull i
  tidslinjen, åpne perioder og et negert institusjonsvilkår gir nok fallgruver
  til at logikken må bo ett sted og være testbar.

Beslutningen tas nå fordi satssteget skal bygges på toppen av utfallet fra
vilkårssteget. Hvert steg som tolker vilkårsradene på egen hånd øker kostnaden
ved å rette dette senere.

### Begrensninger

- Applikasjonen kjører kun i `dev-gcp`. Det finnes ingen produksjonsdata.
- Diagnose er helseopplysning etter GDPR artikkel 9 og kan ikke lekke inn i
  endringshistorikk, logger eller generiske responser.
- `ApiExceptionHandler` mapper i dag alle feil til én `FeilResponse(melding,
  status)`. Andre klienter leser den kontrakten.
- Tidslinjebiblioteket `no.nav.familie.felles:tidslinje` kaster ved overlapp i
  `tilTidslinje()`.

## Beslutning

Backend eier vilkårssteget. Vi innfører en operasjon `fullførSteg(behandlingId)`
som validerer vilkårsvurderingen, utleder hvilke perioder som oppfyller alle
vilkår, og lagrer resultatet.

### Kjernelogikken er rene funksjoner uten Spring

Validering og utledning skrives som rene funksjoner. Inn går én datastruktur
(`VilkårGrunnlag`), ut går en liste med utledede perioder — eller en
`VilkårValideringFeil` hvis vurderingen ikke er komplett.

```
VilkårVurderingStegController      (Spring, HTTP)
        │
VilkårVurderingStegService         (Spring, @Transactional, I/O)
        │  hent    ──────────►  VilkårGrunnlag          (ren datastruktur)
        │  utled   ──────────►  PeriodeMedRettUtleder.utled(...)          ← ren
        │                          └─ VilkårVurderingValidering.valider(...)  ← ren
        │  lagre   ──────────►  PeriodeMedRettRepository
```

Regelen er at ingenting under «ren» importerer `org.springframework`. Ingen
repository-kall, ingen klokke, ingen `SikkerhetContext`. Sporbarhet settes av
`Sporbar`-default ved lagring.

Begrunnelsen er testbarhet. Utledningen har en sannhetstabell med mange
kombinasjoner — overlappende diagnoser, hull, åpne ender, negert
institusjonsvilkår. Med en ren funksjon testes hver kombinasjon uten Spring-
kontekst og uten mocks, og testene beskriver regelen i stedet for
kallrekkefølgen. Spring-klassen står igjen som et tynt skall som gjør I/O og
transaksjonsstyring, og trenger bare én mockk-test for rekkefølgen.

### Valideringen kalles fra utledningen, ikke fra servicen

`PeriodeMedRettUtleder.utled` validerer grunnlaget først og kaster
`VilkårValideringFeil` hvis noe mangler. Servicen kaller bare `utled`.

Begrunnelsen er at et ukomplett grunnlag ikke har noen meningsfull utledning: et
vilkår uten vurderte perioder ville gitt tom liste, som ellers betyr avslag. Ved
å la de to henge sammen kan ingen kaller få det avslagssvaret ved et uhell, og
sammenhengen «ukomplett inn ⇒ feil ut» testes i én ren test uten Spring, i
stedet for å måtte verifiseres gjennom en mockk-test av servicen.

`VilkårVurderingValidering` består som eget objekt, slik at reglene fortsatt kan
testes isolert med enkle sammenligninger.

### Resultatet lagres i egen tabell

Utfallet lagres i `periode_med_rett` i stedet for å utledes på nytt ved hvert
oppslag. Tabellen følger konvensjonene fra `V35`: `TEXT` framfor `VARCHAR(n)`,
`ON DELETE CASCADE` fra `behandling`, CHECK på datorekkefølge og indeks på
`behandling_id`. Entiteten implementerer `Periodisert` fra `Vilkår.kt`, slik at
tidslinje-hjelperne kan gjenbrukes.

`NULL` i datofeltene betyr uendelig, på samme måte som i vilkårstabellene.
Tidslinjebiblioteket gir `fom = null` / `tom = null` for åpne ender, og vi
oversetter ikke dette til magiske datoer som `0001-01-01`.

### Idempotens gjennom slett og sett inn på nytt

`fullførSteg` sletter alle rader for behandlingen og setter inn de utledede på
nytt, i samme transaksjon. Vi differ ikke mot eksisterende rader.

Gjentatte trykk på Neste, og redigering av et vilkår etterfulgt av nytt Neste,
skal alltid gi samme resultat. En diff ville gitt identisk utfall til en høyere
kompleksitet, og med `id` som bare er intern nøkkel er det ingenting å bevare.

### Valideringsfeil er data, og pakkes til exception av utlederen

`VilkårVurderingValidering.valider` returnerer `List<Valideringsfeil>`, der hver
feil bærer `vilkårType` og melding. Frontend kan da vise en `ErrorSummary` med
én lenke per vilkår, og testene blir enkle sammenligninger.

`PeriodeMedRettUtleder.utled` pakker en ikke-tom liste i `VilkårValideringFeil`
og kaster, slik at feilen ikke kan glemmes av en kaller.

Fordi `ApiExceptionHandler` i dag kollapser alt til én melding, innfører vi en
egen `VilkårValideringFeil` med egen handler som returnerer listen med status
400. `FeilResponse` endres ikke — andre klienter leser den.

Handleren legges som en lokal `@ExceptionHandler` i
`VilkårVurderingStegController`, ikke i `ApiExceptionHandler`. Springdoc
behandler handlere i `@ControllerAdvice` som generiske og dytter 400-responsen
inn på samtlige endepunkter i APIet; en lokal handler dokumenteres kun på det
endepunktet den gjelder for, og 200-responsen utledes fortsatt fra returtypen.
Dermed blir `VilkårValideringFeilResponse` en typet feil i den genererte
klienten (`FullførStegError`) i stedet for `unknown`.

Regler ved innføring:

| # | Regel | Hjemmel/kilde | Status |
|---|---|---|---|
| R1 | Minst én vurdert periode per vilkår | Dagens frontendregel `alleVilkårErUtfylt` | Innføres |
| R2 | `unntakshjemmel` uten `oppholdstype` er ugyldig | § 6-8 | Allerede håndhevet i `VilkårInstitusjonService` og CHECK-constraint. Ikke repetert |
| R3 | Begrunnelse påkrevd ved `NEI` | Forvaltningsloven § 25 | Avklart: innføres ikke nå |
| R4 | Krav om sammenhengende dekning av søknadsperioden | — | Avklart: innføres ikke. En periode uten vurdering betyr at vilkåret ikke er oppfylt |

### Tom resultatliste er et lovlig utfall

At ingen perioder oppfyller alle vilkår er **ikke** en valideringsfeil. Det er et
avslagsløp. ADR-0003 slår fast at vilkårssteget alene ikke konkluderer med
innvilgelse — satssteget kan fortsatt gi avslag etter § 6-3 tredje ledd.

### Utledningsreglene

Utledningen bygger én tidslinje per vilkår og kombinerer dem. Tre regler følger
av modellen i ADR-0003 og må håndheves:

1. **Én tidslinje per diagnose.** ADR-0003 tillater flere samtidige diagnoser;
   overlappforbudet gjelder bare innenfor samme diagnose. Å legge alle
   diagnoserader på én tidslinje får `tilTidslinje()` til å kaste. Vi grupperer
   på `diagnose.trim().lowercase()` — samme nøkkel som overlappforbudet bruker —
   og kombinerer gruppene med OR: minst én oppfylt diagnose er nok.
2. **Hull betyr ikke oppfylt.** Der et vilkår mangler periode gir `kombinerMed`
   `null`. Det skal gi *ingen rett*, ikke «ukjent». Sammenligningen er `== true`,
   aldri `!= false`. Dette er også svaret på R4: saksbehandler tvinges ikke til å
   dekke hele søknadsperioden, fordi en periode uten vurdering i seg selv betyr
   at vilkåret ikke er oppfylt.
3. **Institusjonsvilkåret negeres ikke på nytt.**
   `IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM` har `JA` = oppfylt, slik
   ADR-0003 forklarer. Utledningen bruker `erVilkårOppfylt()` og legger ikke inn
   en egen negering.
4. **§ 6-9 lemper medlemskapskravet ved yrkesskade.** Medlemskaps- og
   diagnosevilkåret vurderes derfor i sammenheng, i
   `List<VilkårMedlemskap>.erOppfyltTidslinje(diagnoser)`. Var medlemskapet
   oppfylt da skaden oppsto, og er diagnosen merket som yrkesskade, er
   medlemskapskravet oppfylt ut diagnoseperioden, selv om medlemskapet faller
   bort underveis. Uten yrkesskade kreves medlemskap i den samme perioden som
   diagnosen.
5. **En yrkesskade må vurderes i én periode (forenkling).** Er den samme diagnosen
   delt opp — av et opphold, en periode vurdert til `NEI` eller bare en ny
   vurdering — finnes det flere mulige skadedatoer å måle medlemskapet på.
   Hvilken som gjelder er ikke avklart med fag, så `medMedlemskapskrav` kaster
   «En yrkesskade fordelt på flere perioder støttes ikke enda» i stedet for å
   gjette. Dette er en midlertidig begrensning, ikke en regel fra loven.
6. **En yrkesskade må ha en fra og med-dato.** Uten skadedato finnes det ikke noe
   tidspunkt å måle medlemskapet på. `medMedlemskapskrav` kaster når datoen
   mangler, og `VilkårDiagnoseService.valider` avviser det samme med `400` på
   skrivesiden, slik at dataene aldri blir lagret.

### Endringshistorikk uten helseopplysninger

`EndringType.VILKÅRSVURDERING_FULLFØRT` registreres ved fullføring. `detaljer`
skal kun inneholde antall perioder og datoer — **aldri diagnose**. Dette følger
av GDPR artikkel 9 og av KDoc-en på `VilkårPeriodeService.registrerEndring`.

### Frontend slutter å avgjøre om man får gå videre

`vilkårStatus.alleVilkårErUtfylt` skal ikke lenger blokkere Neste-knappen.
Frontend kaller endepunktet, navigerer først ved suksess, og mater feilene fra
responsen inn i den eksisterende `ErrorSummary`. Visningen av antall perioder
kan beholdes.

Endepunktet følger URL-konvensjonen i repoet, der vilkårsressurser nestes under
behandlingen: `POST /api/behandling/{behandlingId}/vilkar/fullfor`.

## Avgrensninger

| Tema | Hvor det hører hjemme |
|---|---|
| Beløp, sats og nødvendige ekstrautgifter (§ 6-3) | Satssteget |
| Endring av `BehandlingStatus` ved fullføring | Egen beslutning, se åpne spørsmål |
| Et generelt stegrammeverk for alle steg i behandlingen | Utsatt til flere steg er flyttet til backend |
| Fagverifisering av institusjonsenumene | Åpent aksjonspunkt i ADR-0003 |

Vi bygger ikke et generelt stegbegrep nå. Ett steg flyttes til backend, og
abstraksjonen utledes eventuelt når det andre steget kommer.

## Alternativer vurdert

### Alternativ A: Rene funksjoner bak et tynt Spring-skall ✅ (valgt)

- **Fordeler:** Utledningen testes uttømmende uten kontekst og uten mocks.
  Reglene ligger ett sted og kan gjenbrukes av beregning og brev.
- **Ulemper:** Flere filer, og grunnlaget må hentes samlet før kallet.
- **Nav-vurdering:** Rettslig logikk skal være lesbar og etterprøvbar. Skillet
  mellom regel og infrastruktur er det som gir den egenskapen.

### Alternativ B: All logikk i `@Service`-klassen

- **Fordeler:** Færre filer.
- **Ulemper:** Hver regeltest krever mockk-oppsett av tre services og et
  repository. Sannhetstabellen blir i praksis ikke dekket.
- **Nav-vurdering:** Forkastet. Testkostnaden treffer nøyaktig den koden som har
  høyest rettslig risiko.

### Alternativ C: Utlede periodene ved hvert oppslag, uten lagring

- **Fordeler:** Ingen migrering, ingen fare for utdaterte rader.
- **Ulemper:** Ingen lagret konklusjon å bygge beregning og brev på, ingen
  sporbarhet på hva saksbehandler faktisk fullførte, og reglene må kjøres på nytt
  ved hver lesning.
- **Nav-vurdering:** Forkastet. Vi trenger et etterprøvbart utfall per
  behandling.

### Alternativ D: Beholde steglogikken i frontend

- **Fordeler:** Ingen endring.
- **Ulemper:** Regelen kan omgås av klienten, og TODO-en i `vilkår.tsx` blir
  stående.
- **Nav-vurdering:** Forkastet. Saksbehandlingsregler skal håndheves i backend.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

- Diagnose er helseopplysning etter GDPR artikkel 9. Den skal ikke inn i
  `periode_med_rett`, i endringshistorikk eller i feilmeldinger.
- Valideringsfeil identifiserer vilkåret med `vilkårType`, ikke med innholdet i
  vilkårsraden.
- Endepunktet krever `SAKSBEHANDLER`-rolle, og `fullførSteg` sjekker både at
  behandlingen er redigerbar og at kallet kommer fra ansvarlig saksbehandler,
  før noe skrives.
- Sporbarhet settes av `Sporbar` ved lagring, slik at den rene koden ikke trenger
  tilgang til innlogget bruker.

### Plattform

- Endringen krever ingen nye Nais-ressurser og ingen endring i `accessPolicy`.
- Én ny Flyway-migrering (`V36`) oppretter `periode_med_rett`. Eldre migreringer
  røres ikke.
- `EndringType` lagres i en `VARCHAR(100)`-kolonne uten CHECK, så en ny verdi
  krever ingen migrering.

### Team-påvirkning

- Frontend må regenerere API-klienten med `npm run generate:api` etter at backend
  er merget; springdoc-spesifikasjonen er kilden, jf. ADR-0002.
- Beregning og brev får et stabilt inngangspunkt i stedet for å tolke
  vilkårsrader selv.

### Migrasjon

- **Bakoverkompatibilitet:** `FeilResponse` er uendret. Eksisterende
  vilkårsendepunkter er uendret; `/vilkar/fullfor` kolliderer ikke med de mer
  spesifikke stiene `/vilkar/medlemskap`, `/vilkar/diagnose` og
  `/vilkar/institusjon`.
- **Utrulling:** Backend først, deretter regenerering og frontend.
- **Feature toggle:** Ikke nødvendig. Løsningen kjører kun i `dev-gcp` og har
  ingen produksjonsdata.
- **Tilbakerulling:** Vanlig revert. Tabellen er ny og leses ikke av andre.
- **Exit criteria:** TODO-en i `vilkår.tsx` er borte, frontend blokkerer ikke
  lenger på egen hånd, og utlederen har dekket sannhetstabellen med tester.

## Konsekvenser

### Positive

- Kravet om komplett vilkårsvurdering håndheves i backend og kan ikke omgås.
- Utfallet lagres, er sporbart og kan leses av satssteget, beregning og brev.
- Reglene er rene funksjoner og kan testes uttømmende uten Spring.
- Idempotensen gjør at redigering og gjentatte trykk på Neste er trygge.

### Negative

- Feilkontrakten får en ny responstype ved siden av `FeilResponse`.
- `periode_med_rett` er avledede data som blir utdatert hvis et vilkår endres
  uten at steget fullføres på nytt.
- Én ekstra rundtur før navigering; knappen må deaktiveres mens kallet pågår.

### Risiko

Den største risikoen er faglig, ikke teknisk: § 6-9 om yrkesskade lemper
medlemskapskravet i utledningen. Feil her gir rettslig gale vedtaksperioder.
Regelen er implementert med hjemmelsreferanse i koden og dekket av tester i
`MedlemskapOgDiagnoseTidslinjeTest`.

Den nest største er at `periode_med_rett` kan bli utdatert. Den reduseres av at
tabellen alltid skrives i sin helhet ved fullføring, og at ingenting leser den før
satssteget bygges.

## Åpne spørsmål

1. **Låsing av steget.** Skal `fullførSteg` også sette `BehandlingStatus`?
   Anbefalingen er nei i denne omgang. **Fortsatt åpent.**

## Avklarte spørsmål

| Spørsmål | Avklaring |
|---|---|
| Sporing av hjemmel — `kilde`-kolonne i `periode_med_rett` | Innføres ikke. En referanse til en diagnoserad er personopplysning etter artikkel 9, og ingenting leser tabellen ennå |
| Navnet `PeriodeMedRett` vs `PeriodeMedOppfylteVilkår` | `PeriodeMedRett` beholdes. KDoc på entiteten presiserer at perioden ikke er en konklusjon om innvilgelse |
| R4 — sammenhengende dekning av søknadsperioden | Innføres ikke. En periode uten vurdering betyr at vilkåret ikke er oppfylt, og gir dermed ingen rett |
| R3 — begrunnelse ved `NEI` | Innføres ikke nå |
| § 6-9 — skal yrkesskade lempe medlemskapskravet? | Ja. Lempingen ligger i utledningen: er diagnosen en yrkesskade og medlemskapet var oppfylt da skaden oppsto, er medlemskapskravet oppfylt ut diagnoseperioden. En yrkesskade fordelt på flere perioder støttes ikke enda, og utledningen kaster i stedet for å gjette skadedato |

## Aksjonspunkter

- [x] Godkjenn eller forkast ADR-en i teamet.
- [x] Avklar § 6-9 (yrkesskade) med fag.
- [x] Avklar navnevalget.
- [x] Avklar R3 og R4 med fag.
- [x] Implementer backend i tråd med
      [oppgavebeskrivelsen](../oppgaver/vilkarssteg-periode-med-rett.md).
- [ ] Implementer frontend i tråd med oppgavebeskrivelsen.
- [ ] Oppdater ADR-0003 når aggregeringen er på plass.
