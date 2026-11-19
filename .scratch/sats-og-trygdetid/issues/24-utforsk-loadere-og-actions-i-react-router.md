# Utforsk loadere og actions i React Router for data i behandlingen

Status: ready-for-human

## Bakgrunn

Oppfølging av ticket 22 ([ADR-0017](../../../docs/adr/ADR-0017-backend-eier-naavaerende-steg.md)). Tas i en egen PR, fordi valget gjelder hele frontend.

Fanene i en behandling låses ut fra `behandling.gjeldendeSteg`. Frontend må derfor hente behandlingen på nytt etter alt som kan flytte steget. I dag skjer det på fire måter:

- `behandlingLayout.tsx` henter behandlingen i en effekt med `useState` og `AbortController`, og gjør det på nytt når fanen blir synlig (`visibilitychange`).
- Lagring av årsak og fullføring av inngangsvilkår og sats og trygdetid kaller `hentBehandlingPåNytt` før navigering.
- De fire hookene for perioder (`useMedlemskapVilkår`, `useDiagnoseVilkår`, `useEkstrautgifterVilkår`, `useTrygdetidApi`) kaller `hentBehandlingPåNytt` etter lagring og sletting.
- En effekt i layouten omdirigerer en låst fane i URL-en etter at siden er vist.

Svakheten er at hver ny side som endrer grunnlaget, må huske å kalle `hentBehandlingPåNytt`. Glemmer den det, viser fanene feil lås. Backend avviser fortsatt skrivingen, men simulering kan vise en gammel beregning.

React Router 8 (8.5.0) i framework mode har `clientLoader`, `clientAction`, `useFetcher`, `useRouteLoaderData`, `shouldRevalidate` og `redirect`. Etter en action kjører React Router alle aktive loadere på nytt. Det kan erstatte de manuelle kallene uten en ny avhengighet. TanStack Query er alternativet. Det gir invalidering fra ett sted (`MutationCache`), sammenslåing av like kall og henting ved fokus, men det er en ny avhengighet og en annen datamodell.

## Oppgave

Lag en prototype for én fane, sats og trygdetid, og vurder om modellen bør brukes i hele behandlingen.

- Hent behandlingen i en `clientLoader` i `behandlingLayout.tsx`, og les den med `loaderData` eller `useRouteLoaderData`.
- Flytt omdirigeringen fra låste faner til `clientLoader` i fanen, med `throw redirect(…)`, så siden aldri vises.
- Skriv lagring og sletting av trygdetid og ekstrautgifter om til `clientAction` med `useFetcher`. Sjekk at layouten henter behandlingen på nytt uten `hentBehandlingPåNytt`.
- Bytt hentingen ved fokus til `revalidator.revalidate()`.
- Finn ut hvordan valideringsfeil (400 med `melding`) og feiloppsummeringen fra ticket 23 kan vises når lagringen går gjennom en action.
- Mål hvor mange loadere som kjøres per lagring, og hvordan `shouldRevalidate` kan begrense det.
- Sammenlign med TanStack Query på de samme punktene: kode som forsvinner, kode som kommer til, feilhåndtering og hvor mye som må skrives om.

## Spørsmål som skal besvares

- Blir koden enklere eller bare annerledes? Hvor mye av `useApiKall` (10 filer), `VilkårContext` og hookene for perioder må skrives om?
- Kan loadere og hookene som henter selv leve side om side mens vi flytter over, uten at data blir utdatert?
- Hvordan virker `clientLoader` sammen med lesevisning, ansvarlig saksbehandler og totrinnskontroll i høyremenyen?
- Gir React Router det vi trenger, eller veier fordelene med TanStack Query opp for en ny avhengighet?

## Akseptkriterier

- [ ] Prototypen for sats og trygdetid er prøvd i nettleser: lagring av trygdetid etter fullføring låser simulering og brev uten egne kall, og en direkte URL til en låst fane omdirigeres før siden vises.
- [ ] Spørsmålene over er besvart i denne ticketen.
- [ ] Anbefalingen (React Router, TanStack Query eller som i dag) er skrevet som en ADR. En ny avhengighet besluttes av teamet før den legges til.
- [ ] Prototypen merges ikke uten at ADR-en er godkjent.

## Berørt kode

- `apps/frontend/app/routes/behandling/behandlingLayout.tsx`
- `apps/frontend/app/routes/behandling/satsOgTrygdetid.tsx`
- `apps/frontend/app/komponenter/behandling/satsOgTrygdetid/trygdetid/useTrygdetidApi.ts`
- `apps/frontend/app/komponenter/behandling/satsOgTrygdetid/ekstrautgifter/useEkstrautgifterVilkår.ts`
- `apps/frontend/app/komponenter/behandling/vilkår/felles/useApiKall.ts`
- `apps/frontend/app/fellesContext/BehandlingContext.tsx`

## Avgrensning

Ingen endringer i backend, i tilgang eller i hvilke personopplysninger som hentes. Loadere og actions kaller de samme genererte API-funksjonene som i dag.
