# Bruk typede valideringsnøkler i feiloppsummeringen for sats og trygdetid

Status: ready-for-human

## Bakgrunn

Oppfølging av kommentarene i [PR #159](https://github.com/navikt/grunn-og-hjelpestonad/pull/159). Tas i en egen PR, atskilt fra refaktoreringene av beregningsvalideringen og fra stegstyringen i ticket 22.

Feilnøkkelen i API-responsen er en streng. Siden for sats og trygdetid bruker betingelser og nestede ternary-uttrykk for lenker og klikkhåndtering. Siden håndterer både `vilkår` og `inngangsvilkår`, og React-nøkkelen inneholder en indeks.

Spec: [Sats og trygdetid](../spec.md).

## Oppgave

- Eksponer de tillatte responsnøklene som en enum i backend og regenerer API-typer, klient og Zod-skjema med `pnpm run generate:api`. Ikke rediger genererte filer for hånd.
- Bruk den genererte nøkkeltypen i frontend. Navnet `Valideringsnøkkel` i `Record<Valideringsnøkkel, …>` viser til responsens nøkkeltype, ikke den eksisterende interne Kotlin-typen med samme navn.
- Samle lenker og tilhørende navigasjons- eller åpningshandlinger i en tabell typet som `Record<Valideringsnøkkel, …>`. Erstatt betingelsene som velger oppførsel ut fra nøkkelen.
- Fjern den ubrukte `vilkår`-grenen. Behold håndtering av `inngangsvilkår`, `ekstrautgifter` og `trygdetid`.
- Bruk feilnøkkelen alene som React-`key`, uten indeks. Sørg for at responsen har høyst én feil per nøkkel uten å miste relevante feilmeldinger. Inngangsvilkår kan i dag gi flere feil med nøkkelen `inngangsvilkår`, så dette må håndteres eksplisitt.
- La overskriften følge formuleringen og stilen i steg 1, med ordvalg som også dekker trygdetid.

## Akseptkriterier

- [ ] OpenAPI og genererte typer angir nøyaktig hvilke feilnøkler som er tillatt. Backend mapper interne feil eksplisitt, uten en reservegren som behandler ukjente nøkler som ekstrautgifter.
- [ ] En ny responsnøkkel gir TypeScript-feil i tabellen etter at API-typene er regenerert, inntil siden håndterer nøkkelen.
- [ ] Ingen nøkkelbaserte ternary-uttrykk eller foreldreløs `vilkår`-gren gjenstår i feiloppsummeringen.
- [ ] Høyst én feil per nøkkel er sikret og testet, også når både medlemskap og diagnose mangler. Feilinformasjonen beholdes, og React-`key` er unik uten indeks.
- [ ] Lenker navigerer til inngangsvilkår eller åpner og fokuserer riktig seksjon for ekstrautgifter og trygdetid, også ved gjentatte klikk.
- [ ] Feiloppsummeringen får fortsatt fokus ved valideringsfeil. Overskriften følger steg 1.
- [ ] Backend-tester dekker responsnøkler og flere inngangsvilkårsfeil. Frontend verifiseres med `pnpm exec tsc`, relevante tester, lint og formatkontroll. Feilnavigeringen prøves i nettleseren.

## Berørt kode

- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/tilkjentytelse/SatsOgTrygdetidController.kt`
- `apps/frontend/app/routes/behandling/satsOgTrygdetid.tsx`
- `apps/frontend/app/routes/behandling/vilkår.tsx` som referanse for feiloppsummeringen
- `apps/frontend/app/api/generated/`

## Avgrensning

Ingen nye beregningsregler eller stegstyring. Behold generell feilvisning ved andre API-feil. Ingen nye logger eller metrikker er nødvendige.

## Comments

Brukeren ønsker denne oppryddingen i en egen PR.
