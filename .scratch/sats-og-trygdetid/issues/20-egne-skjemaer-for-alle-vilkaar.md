# Egne skjemaer for alle vilkår

Status: ready-for-agent

## Bakgrunn

Ekstrautgifter har fått et eget `EkstrautgifterSkjema`. `VilkårPeriodeSkjema` styrer fortsatt skjemaene for diagnose og medlemskap gjennom generiske props og innhold som sendes inn fra vilkårskomponentene.

## Oppgave

Gi hvert gjenværende vilkår sitt eget skjema med eksplisitt feltrekkefølge og vilkårsspesifikk UI-logikk. Fjern deretter `VilkårPeriodeSkjema` og typer og props som ikke lenger brukes.

- Kartlegg alle brukere av `VilkårPeriodeSkjema`, inkludert diagnose og medlemskap.
- Flytt eksisterende skjemaoppførsel til egne komponenter for hvert vilkår.
- Behold `useVilkårSkjema` og andre delte hooks der de fortsatt passer. Oppgaven krever ikke duplisering av felles tilstand, datohåndtering eller validering.
- Ikke endre forretningsregler, API-kontrakter eller lagrede data.

## Akseptkriterier

- Ingen komponenter bruker `VilkårPeriodeSkjema`, og filen er slettet.
- Hvert vilkår har eksplisitt JSX for egne felt uten generiske innholdsprops fra den gamle komponenten.
- Oppretting, redigering, avbryt og lagring virker som før for hvert berørt vilkår.
- Periodefelt, begrunnelse, vilkårsspesifikke felt og valideringsmeldinger har uendret oppførsel.
- Feiloppsummeringen følger feltrekkefølgen, lenker til riktige felt og får fokus ved ugyldig innsending.
- Lagringsfeil varsles til skjermlesere, og lastetilstand og eksisterende DOM-id-er er bevart.
- Kjør `pnpm exec tsc`, `pnpm test`, `pnpm run lint` og `pnpm run format:check` i `apps/frontend`. Skill eksisterende feil fra regresjoner.
- Test berørte skjemaer i nettleseren, inkludert ugyldig innsending og lagringsfeil. Eksisterende enhetstester dekker ikke alene skjemaflyten.

## Avgrensning

Dette er en refaktorering som skal bevare oppførselen. Nye forretningsregler og endringer i backend tas i egne oppgaver. Ingen nye logger eller metrikker er nødvendige; behold eksisterende feilvisning.

## Comments
