# ADR-0012: Arv og gjeldende perioder fra Infotrygd

**Dato:** 2026-10-27
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`POST /api/infotrygd/stonader` gir stønadene og vedtakene fra Infotrygd som et
tre, uten tolkning. To ting manglet:

- Maskinvedtak mangler ofte evaluering (EVGH) og trygdetid og sats (`t_gh`), se
  [README for infotrygd](../../apps/infotrygd/README.md#kjente-tilfeller-uten-evaluering).
  Frontend fylte inn verdien fra nærmeste tidligere vedtak på samme stønad,
  sortert på `tidspunktReg`.
- Vi hadde ingen måte å se hvilket vedtak som gjelder på en gitt dato.
  [ADR-0005](ADR-0005-perioder-fra-infotrygd.md) lot det stå åpent hvordan vedtak
  overskriver hverandre.

## Beslutning

### Arv i `sak`

`InfotrygdArv.medArv` er en ren funksjon (se [ADR-0008](ADR-0008-deterministisk-logikk-utenfor-spring.md)).
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

1. Hvert vedtak, med arv, blir én periode fra `datoInnvFom` til `datoInnvTom`.
   Tom `datoInnvTom` betyr at perioden er løpende.
2. Et vedtak med `datoInnvTom` før eller lik `datoInnvFom` gir `UgyldigInput`
   (400) med vedtakId, fom og tom. Vi gjetter ikke på hva vedtaket skulle
   gjelde, og saksbehandleren må lese saken i Infotrygd.
3. Vedtakene legges på tidslinjen etter stigende `vedtakId`. Et vedtak
   overskriver de eldre i hele sin periode, på tvers av stønadene. Etter at et
   nyere vedtak slutter, gjelder det eldre igjen.
4. `datoOpphor` på stønaden brukes ikke.
5. Tidsrom uten vedtak er ikke med i svaret.

Hver periode har `fom`, `tom`, `stonadId` og hele vedtaket med arv.

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

### Kutte perioden ved `datoOpphor`

Vi bruker bare `datoInnvTom`. Et nyere vedtak overskriver det eldre.

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

### Risiko

Hvis `vedtakId` ikke stiger med tiden, overskriver et eldre vedtak et nyere.
Følg med på advarselen «vedtak fra Infotrygd er registrert før vedtaket med
nærmeste lavere vedtakId» i loggene.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [ ] Sjekk loggene for avvik i rekkefølgen etter deploy til dev.
- [x] Vis gjeldende perioder i frontend.
