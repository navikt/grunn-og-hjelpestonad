# ADR-0012: Arv og gjeldende perioder fra Infotrygd

**Dato:** 2026-10-27, endret 2026-10-30
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

Endringen 2026-10-30 kommer fra
[ticket 03](../../.scratch/migrering/issues/03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md):
kutt ved opphør, nye feil, sammenslåing og Infotrygd-beløp. Migreringen
([ADR-0013](ADR-0013-migrering-fra-infotrygd-som-i-ef.md)) bruker de samme
periodene. Ticket 26 og 27 bygger endringen, så koden følger ikke ADR-en før de
er ferdige.

## Kontekst

`POST /api/infotrygd/stonader` gir stønadene og vedtakene fra Infotrygd som et
tre, uten tolkning. To ting manglet:

- Maskinvedtak mangler ofte evaluering (EVGH), se
  [README for infotrygd](../../apps/infotrygd/README.md#kjente-tilfeller-uten-evaluering).
  Frontend fylte inn verdien fra nærmeste tidligere vedtak på samme stønad,
  sortert på `tidspunktReg`. Trygdetid og sats (`t_gh`) mangler sjelden: i dev
  mangler bare 2 vedtak som ikke er annullert, `t_gh`, og ingen av dem er
  maskinvedtak.
- Vi hadde ingen måte å se hvilket vedtak som gjelder på en gitt dato.
  [ADR-0005](ADR-0005-perioder-fra-infotrygd.md) lot det stå åpent hvordan vedtak
  overskriver hverandre.

## Beslutning

### Arv i `sak`

`InfotrygdArv.fyllVedtakMedVerdierFraTidligereVedtak` er en ren funksjon (se [ADR-0008](ADR-0008-deterministisk-logikk-utenfor-spring.md)).
Den erstatter logikken i frontend.

- Et vedtak uten egen evaluering eller `gh` arver verdien fra vedtaket med
  nærmeste lavere `vedtakId` som har en egen verdi.
- Arven går på tvers av alle stønadene til personen for stønadstypen.
- Evaluering og `gh` arves hver for seg og som hele objekter.
- Arv går ikke i kjede. Kilden er alltid et vedtak med egen verdi.
- Vedtak med `datoInnvTom` før eller lik `datoInnvFom` kan også være kilde.

I API-et er `evaluering` og `gh` av typen `VerdiMedKilde<T>?`, med formen
`{ verdi, arvetFraVedtak }`. `arvetFraVedtak` er `null` når verdien er vedtakets
egen. Feltet er `null` når verken vedtaket eller et tidligere vedtak har en
verdi, så `gh` er aldri en tom liste.

### Gjeldende perioder

`GjeldendeInfotrygdPerioder.utled` bruker tidslinjebiblioteket fra
`familie-felles`. Resultatet kommer ut på `POST /api/infotrygd/gjeldende-perioder`.
Migreringen bruker de samme periodene, slik at saksbehandler ser i oppslaget hva
migreringen vil gjøre.

1. Hvert vedtak, med arv, blir én periode fra `datoInnvFom` til `datoInnvTom`.
   Tom `datoInnvTom` betyr at perioden er løpende.
2. Et vedtak med `datoInnvTom` før eller lik `datoInnvFom` gir `UgyldigInput`
   (400) med vedtakId, fom og tom. Vi gjetter ikke på hva vedtaket skulle
   gjelde, og saksbehandleren må lese saken i Infotrygd.
3. Når stønaden har `datoOpphor`, kuttes tom på alle vedtakene på stønaden ved
   opphøret. Er opphørsdatoen den 1. i måneden, blir tom siste dag i måneden
   før, som i EF. Andre datoer brukes som de er.
4. Et vedtak som får tom før fom etter kuttet, gir `UgyldigInput`: «Vedtak
   {vedtakId} starter {fom}, etter at stønaden opphørte {opphørsdato}. Vi har
   ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.»
5. Har minst to stønader vedtak uten tom etter kuttet, gir oppslaget
   `UgyldigInput`: «Personen har løpende vedtak på flere stønader i Infotrygd
   ({stonadId-er}). Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt
   teamet.» Er personens siste vedtak avsluttet på eller etter datogrensen for
   migrering (2026-01-01), mens stønaden ikke er opphørt, gir oppslaget
   `UgyldigInput`: «Stønad {stonadId} er ikke opphørt, men siste vedtak
   {vedtakId} sluttet {tom}. Vi har ikke bestemt hvordan slike saker skal
   tolkes. Kontakt teamet.»
6. Mangler vedtaket delytelse, eller dekker delytelsen mindre enn
   vedtaksperioden etter kuttet, gir oppslaget `UgyldigInput`. I den siste
   gjeldende perioden gir det samme feil når delytelsen har en annen periode
   enn vedtaket, også når den dekker mer. I tidligere perioder er en delytelse
   som dekker mer, ikke en feil. Meldingen: «Vedtak {vedtakId} gjelder
   {fom}–{tom}, men delytelsen med beløpet i Infotrygd gjelder
   {delytelseFom}–{delytelseTom}. Vi har ikke bestemt hvordan slike vedtak skal
   tolkes. Kontakt teamet.»
7. Vedtakene legges på tidslinjen etter stigende `vedtakId`. Et vedtak
   overskriver de eldre i hele sin periode, på tvers av stønadene. Etter at et
   nyere vedtak slutter, gjelder det eldre igjen.
8. Perioder som følger etter hverandre, slås sammen når Infotrygd-beløp, sats,
   trygdetid, diagnose og hjemmelbokstav er like.
9. Tidsrom uten vedtak er ikke med i svaret.

Hver periode har `fom`, `tom`, `stonadId`, hele vedtaket med arv og
Infotrygd-beløpet. Beløpet kommer fra grunnstønad-delytelsen på vedtaket, og
vedtaket avgjør perioden.

Sats og trygdetid kommer fra raden i `gh` med nyest `tidspunktReg`.

## Alternativer vurdert

### Rekkefølge på `tidspunktReg`

Frontend sorterte på `tidspunktReg`. Vi bruker `vedtakId`, så arv og tidslinje
følger samme rekkefølge. Vi har ikke bekreftet at `vedtakId` stiger med
registreringstidspunktet. `sak` logger en advarsel med antall vedtak der
rekkefølgen avviker, se Risiko.

### Sealed class for kildefeltene

Vi prøvde en sealed class (`Egen` og `Arvet`) med springdoc 3.1.1 og med
grenen `springdoc-openapi-jackson3`:

- Uten `@JsonTypeInfo` får JSON-en ingen discriminator. Zod-skjemaet i frontend
  kaster da en feil.
- Med egne navn som `EGEN` og `ARVET` sier specen klassenavnet, og frontend
  avviser svaret. Det trengs `discriminatorMapping` eller `Id.SIMPLE_NAME`.
- Generiske sealed-typer gir `verdi: unknown` i TypeScript.
- Med 3.1.1 mister et nullbart sealed-felt `null` i specen.

Wrapperen `VerdiMedKilde<T>` gir samme garanti mot ugyldige tilstander uten
polymorfi i OpenAPI.

### Se bort fra `datoOpphor`

Den første versjonen brukte bare `datoInnvTom`. Da kunne en stønad som er
opphørt i Infotrygd, se løpende ut og bli migrert. Vi kutter derfor ved opphøret,
som EF.

### Overskriving som i EF

`InfotrygdPeriodeUtil` i EF-appen kutter alt etter fom på det nyere vedtaket, så
et eldre vedtak gjelder aldri igjen. EF gjør det per stønad. Vi beholder vår
regel. Etter kuttet ved opphør gir de to reglene ulikt svar for 5 stønader i dev,
alle med siste vedtak mellom 2009 og 2020. Ingen av dem kan migreres.

### Filtrere bort vedtak som faller bort ved opphør

EF filtrerer stille bort vedtak som får tom før fom etter kuttet. I dev gjelder
det 2 122 vedtak hos 398 av 147 482 personer som kan migreres. Ingen av dem er
opphørsvedtak: de har resultat `FI` eller `I` og beløp over 0. Vi vet ikke hva
som er riktig tolkning, og gir derfor feil til vi har sett på tilfellene.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

- Begge svarene inneholder helseopplysninger (GDPR art. 9).
- Det nye endepunktet har samme roller, tilgangssjekk mot personen og
  `Cache-Control: no-store` som `/stonader`. Det står i `MED_PERSONSJEKK`
  ([ADR-0009](ADR-0009-tilgangskontroll-mot-person.md)).
- Loggene viser bare antall og stønadstype. `apps/infotrygd` auditlogger
  oppslagene.

### Plattform

Ingen nye Nais-ressurser.

## Konsekvenser

### Positive

- Arven finnes ett sted og er testet i Kotlin.
- Vi kan slå opp hvilket vedtak som gjelder på en dato.

### Negative

- `/stonader` endret form på `evaluering` og `gh`. Frontend og backend må
  deployes sammen.
- Arv kan gi en evaluering fra en annen stønad eller sak enn vedtaket.
- Oppslaget gir feil for personer med vedtak som faller bort ved opphør, med
  løpende vedtak på flere stønader, med en delytelse som ikke stemmer med
  vedtaket, eller med et nylig avsluttet vedtak på en stønad som ikke er
  opphørt. Saksbehandler ser da ingen gjeldende perioder for personen. I dev
  gjelder det høyst 2 419 av 147 482 personer som kan migreres. En person kan
  ha flere av feilene, så det reelle tallet kan være lavere:

  | Feil | Personer |
  |------|---------:|
  | Delytelsen har en annen periode enn vedtaket i siste periode | 1 942 |
  | Vedtak faller bort ved opphør | 398 |
  | Delytelsen dekker mindre enn vedtaket | 76 |
  | Løpende vedtak på flere stønader | 2 |
  | Siste vedtak avsluttet etter datogrensen, stønaden ikke opphørt | 1 |

  Vi gir heller én feil for mye, fordi reglene er lettere å endre enn en
  migrering som har gått galt.
- Feilmeldingene har vedtakId og stonadId, ikke fnr.

### Risiko

Hvis `vedtakId` ikke stiger med tiden, overskriver et eldre vedtak et nyere.
Følg med på advarselen «vedtak fra Infotrygd er registrert før vedtaket med
nærmeste lavere vedtakId» i loggene.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [ ] Sjekk loggene for avvik i rekkefølgen etter deploy til dev.
- [x] Vis gjeldende perioder i frontend.
- [ ] Få en Infotrygd-ekspert til å bekrefte at et eldre vedtak gjelder igjen
      etter at et nyere slutter.
- [ ] Kutt ved opphør og de nye feilene (ticket 26).
- [ ] Sammenslåing og Infotrygd-beløp (ticket 27).
- [ ] Finn ut hvordan vedtak som faller bort ved opphør, løpende vedtak på flere
      stønader og delytelser som ikke stemmer med vedtaket, skal tolkes.
- [ ] Spør en Infotrygd-ekspert hvorfor en maskinell satsomregning (`MS`) kan
      være første vedtak, og hva en delytelse som slutter før et løpende vedtak
      betyr. Se eksemplene fra saksbehandler i ticket 03.
