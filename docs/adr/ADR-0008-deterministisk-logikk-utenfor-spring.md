# ADR-0008: Deterministisk logikk utenfor Spring

**Dato:** 2026-10-09
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

[ADR-0004](ADR-0004-vilkaarssteg-utleder-perioder-med-rett.md) innførte
regelen om at kjernelogikken i vilkårssteget skrives som rene funksjoner uten
Spring. `PeriodeMedRettUtleder` og `VilkårVurderingValidering` er `object`-er,
og `VilkårVurderingStegService` henter, kaller dem og lagrer.
[ADR-0006](ADR-0006-fem-vilkaar-med-inngangsvilkaar-som-foerste-steg.md)
viderefører regelen.

Regelen gjelder bare vilkårssteget. Andre steder ligger deterministisk logikk
i Spring-bønner:

- `DiagnosekodeService` er en `@Service` uten avhengigheter. Kodeverket ligger
  i et `companion object`, og bønnen er et tomt skall rundt det.
- `VilkårDiagnoseService` og `VilkårInstitusjonService` overstyrer hooks i
  `VilkårPeriodeService` (`valider`, `nyPeriode`, `perioderSomIkkeKanOverlappe`).
  Hookene er regler, men de testes gjennom servicen med mocks.
- `valider` kaster `Feil(httpStatus = HttpStatus.BAD_REQUEST)`. Da importerer
  regelen `org.springframework.http`.

## Beslutning

Vi gjør regelen fra ADR-0004 generell for `apps/sak`.

### Kriteriet er deterministisk

Koden gir samme svar for samme input. Den gjør ikke I/O, leser ikke klokka og
leser ikke `SikkerhetContext`. Data som er pakket inn i appen, som ICD-10 fra
`navikt/diagnosekoder`, teller som input og ikke som I/O.

Ren kode importerer ikke `org.springframework`.

### Hent, behandle, lagre

Servicen henter dataene. Rene funksjoner behandler dem. Servicen lagrer
resultatet. Servicen eier transaksjonen, tilgangskontrollen og
endringshistorikken.

### Når koden flyttes ut

Deterministisk kode flyttes ut av Spring når minst én av disse stemmer:

1. Den har forgreninger du vil teste med mer enn ett tilfelle.
2. Den er en regel fag kan endre, for eksempel en lovregel, et vilkår eller en
   rangering.
3. Den brukes fra mer enn ett sted.

Rene kopier av felt og enlinjere blir der de er.

### Hooks i template-klasser delegerer

Når en Spring-klasse bruker template method, som `VilkårPeriodeService`, blir
skjelettet med I/O og transaksjoner stående. Overstyringene blir enlinjere som
kaller rene funksjoner, for eksempel
`override fun valider(request) = DiagnoseRegler.valider(request)`.

### Ren kode kaster `UgyldigInput`

Ren kode kan ikke kaste `Feil`, fordi den tar `HttpStatus`. Vi innfører
`UgyldigInput(melding: String)` uten avhengighet til Spring.
`ApiExceptionHandler` gjør den om til 400 med `FeilResponse`, samme form som i
dag.

Navnet sier hva som er galt, ikke hvem som har skylden eller hvilken
HTTP-status det blir. `Valideringsfeil` er allerede tatt av ADR-0004.

Handleren logger ikke meldingen og ikke stacktrace, se
[Sikkerhet og personvern](#sikkerhet-og-personvern).

`VilkårValideringFeil` fra ADR-0004 forblir som den er, med egen handler og en
liste med feil per vilkår.

### Konvensjon, ikke arkitekturtest

Vi håndhever regelen i kodegjennomgang. En arkitekturtest kan bare sjekke at
ren kode ikke importerer `org.springframework`. Om koden skal flyttes ut, er en
vurdering etter de tre testene over, og den kan ingen test gjøre.

## Alternativer vurdert

### Regelen gjelder alltid

- **Fordeler:** Ingen vurdering i review.
- **Ulemper:** Rene kopier av felt får en ekstra indireksjon uten å bli
  enklere å teste.

### Strategi i stedet for arv i `VilkårPeriodeService`

Baseklassen tar inn et rent `VilkårRegler`-objekt i stedet for abstrakte
metoder.

- **Fordeler:** Subklassene blir bare oppkobling.
- **Ulemper:** Omskriving av fire klasser og testene deres. Mer struktur enn
  fire vilkår trenger nå.

### Returnere feil i stedet for å kaste

Ren kode returnerer `List<Valideringsfeil>`, og servicen kaster `Feil`, som
`VilkårVurderingValidering`.

- **Fordeler:** Testene sammenligner lister. Servicen eier HTTP-mappingen.
- **Ulemper:** Servicen må huske å sjekke listen.

### `require(...)` og `IllegalArgumentException`

- **Fordeler:** Ingen nye typer.
- **Ulemper:** `ApiExceptionHandler` gjør også `IllegalStateException` om til
  400. Da kan vi ikke skille ugyldig input fra feil i koden.

### Konsist-test i tillegg til konvensjonen

- **Fordeler:** Bygget feiler hvis ren kode importerer Spring.
- **Ulemper:** Ny testavhengighet og en navnekonvensjon for å kjenne igjen ren
  kode. Dekker bare halve regelen.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

Meldingen i en `UgyldigInput` vises til saksbehandler. Diagnose-reglene holder i
dag diagnosekoden utenfor meldingen bare ved at utviklerne husker det, se
kommentaren i `VilkårDiagnoseService.valider`. Diagnosekoden er en
helseopplysning etter GDPR artikkel 9. Siden handleren ikke logger meldingen,
havner den ikke i loggene selv om noen tar den med.

## Konsekvenser

### Positive

- Regler testes med vanlige enhetstester, uten Spring-kontekst og uten mocks.
- Servicene blir tynne og gjør bare I/O, tilgang og transaksjoner.
- Logger inneholder ikke meldinger fra `UgyldigInput`.

### Negative

- En ekstra indireksjon per hook i `VilkårPeriodeService`.
- Når en regel brytes, sier loggen ikke hvilken.

### Risiko

Kriteriet krever en vurdering. Uten en test kan regelen gli ut over tid.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [x] Innfør `UgyldigInput` og handleren i `ApiExceptionHandler`.
- [x] Gjør `DiagnosekodeService` om til `object Icd10`.
- [x] Flytt `VilkårDiagnoseService.valider` og `perioderSomIkkeKanOverlappe` til
      `DiagnoseRegler`. Hva som er «samme diagnose», er en regel fag kan endre.
- [ ] Flytt `VilkårInstitusjonService.valider` og `nyPeriode` til rene
      funksjoner. `nyPeriode` nuller felt når vurderingen er `JA`, og det er en
      regel.
