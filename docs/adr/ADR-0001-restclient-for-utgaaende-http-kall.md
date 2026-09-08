# ADR-0001: RestClient for utgående HTTP-kall

**Dato:** 2026-09-08
**Status:** Implementert
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`sak` er en Spring MVC-applikasjon, men bruker `WebClient` fra WebFlux til
synkrone HTTP-kall med `.block()`. Det gir en reaktiv avhengighet og reaktive
API-er uten at applikasjonen ellers er reaktiv.

Målet er å bruke en HTTP-klient som passer den synkrone kjøremodellen, uten å
endre API-kontrakter eller funksjonell oppførsel. Hvis vi ikke gjør noe, fungerer
løsningen fortsatt, men vi beholder unødvendig teknisk kompleksitet.

## Beslutning

Vi migrerer utgående HTTP-kall i `sak` fra `WebClient` til Spring
`RestClient`. Migreringen gjennomføres klient for klient i én samlet
kodeendring. Når alle kall er migrert, fjernes
`spring-boot-starter-webflux`.

Beslutningen omfatter WebClient- og Reactor-bruk i `sak`. Leader election
bruker fortsatt JDK `HttpClient`, siden dette er et separat, synkront kall uten
WebFlux-avhengighet.

## Alternativer vurdert

### Alternativ A: Bytte til RestClient

- **Fordeler:** Passer Spring MVC, gir synkrone API-er og reduserer antall
  avhengigheter.
- **Ulemper:** Krever migrering av klienter, feilhåndtering og tester.
- **Nav-vurdering:** Reduserer unødvendig kompleksitet og følger prinsippet om
  å velge den enkleste løsningen som dekker behovet.

### Alternativ B: Beholde WebClient og gjøre ingenting

- **Fordeler:** Ingen implementasjonskostnad eller migrasjonsrisiko.
- **Ulemper:** Beholder WebFlux og reaktive API-er i en synkron applikasjon.
- **Nav-vurdering:** Teknisk gyldig, men gir mer kompleksitet enn behovet tilsier.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

- Kallene behandler blant annet personidenter og dokumenter, som skal håndteres
  som fortrolige data.
- Entra ID-token fra Texas skal fortsatt sendes med riktig målgruppe og aldri
  logges.
- Feillogging skal ikke inneholde tokens, personidenter eller komplette
  responsdata med personopplysninger.
- Eksisterende statuskodehåndtering skal videreføres uten bred eller skjult
  feilhåndtering.

### Plattform

- Endringen krever ingen nye Nais-ressurser eller endringer i `accessPolicy`.
- Tilkoblingstimeout settes til 5 sekunder og responstimeout til 10 sekunder
  gjennom felles konfigurasjon.
- Klientene skal bygges fra Spring Boots konfigurerte `RestClient.Builder`, slik
  at standard observability videreføres.
- Eksisterende logging og metrikker for feil og responstid skal bevares.
- Fjerning av WebFlux reduserer applikasjonens avhengighetsflate noe.

### Team-påvirkning

- Endringen er intern i `sak`; konsumenter og eksterne API-kontrakter påvirkes
  ikke.
- Teamet kan gjennomføre og rulle ut migreringen trinnvis.

### Migrasjon

- **Bakoverkompatibilitet:** Eksterne forespørsler og svar skal være uendret.
- **Utrulling:** Klientene migreres og testes én om gangen.
- **Feature toggle:** Ikke nødvendig fordi oppførselen ikke skal endres.
- **Tilbakerulling:** Vanlig tilbakerulling av deploy eller Git-revert.
- **Exit criteria:** Ingen produksjons- eller testkode bruker WebClient eller
  Reactor, og WebFlux-avhengigheten er fjernet.

## Konsekvenser

### Positive

- HTTP-klientene samsvarer med applikasjonens synkrone modell.
- Mindre teknisk kompleksitet og færre transitive avhengigheter.

### Negative

- Migreringen krever endringer i flere klienter og tilhørende tester.
- Timeout- og feilhåndtering må konfigureres på nytt.

### Risiko

Den viktigste risikoen er endret oppførsel for timeouts, tomme responser eller
HTTP-feil. Dette reduseres med karakteriseringstester og trinnvis migrering.

## Aksjonspunkter

- [x] Godkjenn eller forkast ADR-en i teamet.
- [x] Lag karakteriseringstester for statuskoder, tomme svar og timeouts.
- [x] Etabler felles konfigurasjon for `RestClient`, timeouts og feilhåndtering.
- [x] Migrer og verifiser én klient om gangen.
- [x] Kontroller at auth-headere og målgrupper er riktige.
- [x] Fjern Reactor-bruk og `spring-boot-starter-webflux`.
- [x] Oppdater ADR-status og dokumentasjon etter migreringen.

## Implementering

Migreringen ble fullført 8. september 2026. Alle utgående HTTP-klienter i
`sak` bygges fra Spring Boots konfigurerte `RestClient.Builder`.
Tilkoblingstimeout er satt til 5 sekunder og responstimeout til 10 sekunder
gjennom `spring.http.clients`.

Karakteriseringstestene dekker statuskoder, tomme svar, responstimeout,
auth-headere og målgrupper for OBO- og maskintokener.
`spring-boot-starter-webflux` og all bruk av WebClient og Reactor er fjernet.
Leader election bruker fortsatt JDK `HttpClient`.
