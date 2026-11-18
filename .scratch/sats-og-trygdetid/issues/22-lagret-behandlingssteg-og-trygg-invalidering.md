# Lagre behandlingssteg og krev ny fullføring etter grunnlagsendringer

Status: ready-for-human

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

Avklart 2026-11-18. Se [ADR-0017](../../../docs/adr/ADR-0017-backend-eier-naavaerende-steg.md).

- Stegene er `ÅRSAK`, `INNGANGSVILKÅR`, `SATS_OG_TRYGDETID` og `VEDTAK_OG_BREV` i enumen `BehandlingSteg`. Rekkefølgen står i feltet `rekkefølge`, ikke i hvilken rekkefølge verdiene er skrevet. Simulering hører til vedtak og brev og fullføres ikke for seg.
- Medlemskap og diagnose fører tilbake til `INNGANGSVILKÅR`, ekstrautgifter og trygdetid til `SATS_OG_TRYGDETID`. Ny eller endret årsak setter steget til `INNGANGSVILKÅR`. Samme årsak lagret på nytt gjør ingenting.
- Senere faner låses, og backend avviser kallet. Lagret vedtak og mellomlagret brev beholdes inntil videre.
- Uten oppfylte inngangsvilkår går behandlingen rett til `VEDTAK_OG_BREV`. Fullføring av inngangsvilkår svarer med `nesteSteg` som et `BehandlingSteg`, og enumen `NesteStegEtterInngangsvilkår` er fjernet. Underkjenning endrer ikke steget.
- Det finnes ingen åpne behandlinger i prod. Migreringen `V59` setter alle til `ÅRSAK`. Fanene låses bare mens behandlingen er redigerbar, så avsluttede behandlinger kan leses.
- Backend låser ikke behandlingsraden. Bare ansvarlig saksbehandler kan skrive, så vi godtar risikoen for to samtidige kall på samme behandling.
- Ingen ny historikk eller overvåking av avviste steg. Avvisningene logges som håndterte feil uten personopplysninger.

## Akseptkriterier

- [x] Stegomfang og overgangsregler er avklart og dokumentert i ticketen før implementering.
- [x] Periodeendring og tilbakeføring av steg skjer i samme transaksjon. Endringen flytter aldri fremdriften fremover.
- [ ] Tilkjent ytelse beholdes ved periodeendring, men en foreldet beregning kan ikke brukes til å sende behandlingen videre eller opprette utbetaling. Vernet er på plass. Slettingen fjernes i en egen PR.
- [x] Backend avviser forsøk på å omgå nødvendig fullføring med direkte API-kall. Frontend håndterer sperrede faner og direkte URL med forståelig tilbakemelding.
- [x] «Neste» validerer og fullfører steget før fremdriften oppdateres. Fullføring av sats og trygdetid erstatter gammel beregning.
- [x] Tester dekker endring og sletting på hvert berørt steg, allerede tidligere fremdrift, ny beregning, forsøk på å hoppe over steg og de avklarte særflytene.
- [x] Migrering av eksisterende behandlinger, innføring og rollback er beskrevet. Dagens sletting fjernes ikke før det nye backendvernet er på plass.

## Berørt kode

- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/felles/periode/PeriodeMedBegrunnelseService.kt`
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/behandling/` (`Behandling.kt`, `BehandlingSteg.kt`, `BehandlingService.kt`, `årsak/ÅrsakBehandlingService.kt`)
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/steg/vilkårVurdering/InngangsvilkårStegService.kt`
- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/steg/satsOgTrygdetid/SatsOgTrygdetidService.kt`
- `apps/sak/src/main/resources/db/migration/V59__behandling_navarende_steg.sql`
- `apps/frontend/app/routes/behandling/behandlingLayout.tsx`
- `apps/frontend/app/komponenter/navbar/BehandlingFaner.tsx` og `behandlingFaneRegel.ts`
- Hookene for perioder, som henter behandlingen på nytt etter lagring og sletting

## Avgrensning

Ingen endringer i hvem som har tilgang til behandlingen eller hvilke personopplysninger som behandles. Eksisterende tilgangskontroller beholdes. Avklar om det trengs historikk og overvåking av avviste stegoverganger. Loggene skal ikke inneholde personopplysninger.

## Comments

Brukeren valgte å ta stegstyringen i en egen PR. Avklaringene ble besvart 2026-11-18, se over.

2026-11-18: Implementert på branchen `tilkjent-ytelse/lagret-behandlingssteg`, som bygger på PR #159. Ikke committet ennå. Backend har 760 grønne tester. Frontend er grønn på `tsc`, lint og tester.

Valg underveis:

- Reglene er rene funksjoner på `Behandling`: `validerErRedigerbar`, `validerHarNådd` og `validerKanEndre`. `BehandlingService.validerKanEndreSteg` returnerer ingenting. Den som trenger behandlingen, henter den selv.
- Frontend henter behandlingen på nytt etter lagring av årsak, etter fullføring, etter lagring og sletting av perioder og når nettleserfanen blir synlig. Det skjer ikke ved hvert fanebytte. En låst fane i URL-en sender saksbehandler videre med en `InlineMessage`.
- Om loadere og actions i React Router skal ta over hentingen, utforskes i [ticket 24](24-utforsk-loadere-og-actions-i-react-router.md).

Gjenstår:

- `ÅrsakBehandlingService.lagreÅrsakForBehandling` sjekker ikke at behandlingen er redigerbar. En endret årsak på en behandling som er sendt til beslutter eller ferdigstilt, setter status tilbake til `UTREDES`. Det må avklares før merge.
- Simulering har ikke stegsjekk. `BehandlingService.validerHarNåddStegHvisRedigerbar` er ikke i bruk og kan fjernes.
- Svaret `nesteSteg` kan i OpenAPI ha alle fire stegene. `vilkår.tsx` håndterer bare `SATS_OG_TRYGDETID` og `VEDTAK_OG_BREV`.
- Hvis den lokale databasen har en annen V58, startes backend lokalt med `SPRING_FLYWAY_IGNORE_MIGRATION_PATTERNS=*:missing`.
- Flyten er ikke prøvd i nettleser.
- Slettingen av tilkjent ytelse fjernes i en egen PR, som også erstatter ticket 11.
- Et lagret vedtak kan sendes til beslutter etter at grunnlaget er endret. Det bør bli en egen ticket.
