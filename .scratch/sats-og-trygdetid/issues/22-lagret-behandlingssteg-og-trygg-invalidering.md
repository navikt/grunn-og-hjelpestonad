# Lagre behandlingssteg og krev ny fullføring etter grunnlagsendringer

Status: needs-info

## Bakgrunn

Oppfølging av [kommentarene i PR #159](https://github.com/navikt/grunn-og-hjelpestonad/pull/159#discussion_r4611757150). Tas i en egen PR, ikke som del av fullføring av sats og trygdetid.

`PeriodeMedBegrunnelseService` sletter i dag tilkjent ytelse når perioder opprettes, endres eller slettes. Backend lagrer behandlingsstatus, men ikke hvilket arbeidssteg behandlingen har nådd. Frontend tillater direkte navigering til flere senere steg.

Ønsket er å beholde tilkjent ytelse og føre behandlingen tilbake til steget der grunnlaget ble endret. Saksbehandler må fullføre nødvendige steg med «Neste» før behandlingen kan gå videre. En beregning er ikke gyldig bare fordi den er lagret.

Spec: [Sats og trygdetid](../spec.md). Se også [ticket 11 om sletting av tilkjent ytelse](11-tilkjent-ytelse-slettes-naar-vilkaar-endres.md), som denne oppgaven skal erstatte når nytt vern er på plass.

## Oppgave

- Lagre fremdriften for behandlingssteg i backend, atskilt fra behandlingens overordnede status.
- La `PeriodeMedBegrunnelseService` kjenne steget periodene tilhører. Ved lagring og sletting skal behandlingen føres tilbake til riktig steg uten å flyttes fremover dersom den allerede står tidligere i flyten.
- Håndhev i backend at nødvendige steg må fullføres på nytt før senere steg kan fullføres eller behandlingen sendes videre. Frontend skal følge samme fremdrift ved fanevalg, direkte URL og «Neste».
- Erstatt sletting ved periodeendring med en kontroll som hindrer bruk av foreldet tilkjent ytelse. Kontrollen må beskrives før implementering. Ny fullføring av sats og trygdetid skal beregne og lagre ytelsen på nytt.
- Avklar hva endringen betyr for simulering, lagret vedtak og sending til beslutter. Kontrollen av vedtaksresultat mot lagrede andeler er ikke alene et vern mot en foreldet beregning.

## Avklaringer før implementering

- Skal alle steg omfattes fra starten: årsak, vilkår, sats og trygdetid, simulering og brev?
- Skal medlemskap og diagnose føre tilbake til «Vilkår», og ekstrautgifter og trygdetid til «Sats og trygdetid»?
- Skal senere resultater kunne leses som utdaterte, eller skal senere sider være utilgjengelige? Hvilke resultater beholdes utover tilkjent ytelse?
- Hvordan fullføres flyten uten oppfylte inngangsvilkår, som i dag hopper over sats og trygdetid? Hva skjer ved underkjenning fra beslutter?
- Hvordan settes fremdriften for eksisterende åpne behandlinger ved innføring? Hvordan bevares lesevisning for avsluttede behandlinger?

## Akseptkriterier

- [ ] Stegomfang og overgangsregler er avklart og dokumentert i ticketen før implementering.
- [ ] Periodeendring og tilbakeføring av steg skjer i samme transaksjon. Endringen flytter aldri fremdriften fremover.
- [ ] Tilkjent ytelse beholdes ved periodeendring, men en foreldet beregning kan ikke brukes til å sende behandlingen videre eller opprette utbetaling.
- [ ] Backend avviser forsøk på å omgå nødvendig fullføring med direkte API-kall. Frontend håndterer sperrede faner og direkte URL med forståelig tilbakemelding.
- [ ] «Neste» validerer og fullfører steget før fremdriften oppdateres. Fullføring av sats og trygdetid erstatter gammel beregning.
- [ ] Tester dekker endring og sletting på hvert berørt steg, allerede tidligere fremdrift, ny beregning, forsøk på å hoppe over steg og de avklarte særflytene.
- [ ] Migrering av eksisterende behandlinger, innføring og rollback er beskrevet. Dagens sletting fjernes ikke før det nye backendvernet er på plass.

## Berørt kode

- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/felles/periode/PeriodeMedBegrunnelseService.kt`
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/behandling/`
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/steg/vilkårVurdering/InngangsvilkårStegService.kt`
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/tilkjentytelse/BeregnTilkjentYtelseService.kt`
- `apps/frontend/app/routes/behandling/behandlingLayout.tsx`
- `apps/frontend/app/komponenter/navbar/BehandlingFaner.tsx`

## Avgrensning

Ingen endringer i hvem som har tilgang til behandlingen eller hvilke personopplysninger som behandles. Eksisterende tilgangskontroller beholdes. Avklar om det trengs historikk og overvåking av avviste stegoverganger. Loggene skal ikke inneholde personopplysninger.

## Comments

Brukeren valgte å ta stegstyringen i en egen PR. Avklaringene over er fortsatt åpne.
