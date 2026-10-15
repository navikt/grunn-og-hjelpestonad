# ADR-0009: Tilgangskontroll mot person via tilgangsmaskinen

**Dato:** 2026-10-09
**Status:** Akseptert
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`apps/sak` sjekket bare om saksbehandleren hadde en rolle i appen (`SAKSBEHANDLER`,
`BESLUTTER` eller `LESETILGANG`). Ingen endepunkter sjekket om saksbehandleren hadde
tilgang til personen saken gjelder. En saksbehandler kunne dermed slå opp personer
med strengt fortrolig adresse, skjermede Nav-ansatte og seg selv.

Infotrygd i dev har skarpe data, så dev og prod må ha samme tilgangskontroll.

Nav har en felles tjeneste for dette: tilgangsmaskinen (`populasjonstilgangskontroll`
i namespace `tilgangsmaskin`), som team Sikkerhetstjenesten forvalter. Den har to
regelsett:

- **Kjerneregler:** strengt fortrolig adresse (kode 6 og § 19), fortrolig adresse
  (kode 7), skjerming, habilitet (egne data og egen familie) og verge. De kan ikke
  overstyres.
- **Overstyrbare regler:** geografisk tilgang og avdød i mer enn 12 måneder.
  Saksbehandleren kan få enkelttilgang til én person når en av disse avviser.

Dokumentasjonen til tilgangsmaskinen anbefaler komplett regelsett (kjerneregler
og overstyrbare regler) ved oppslag på hovedparten i en sak, og bare kjerneregler
på andre parter.

## Beslutning

Vi sjekker tilgang til stønadsmottaker med tilgangsmaskinen og kjerneregelsettet,
før hvert endepunkt som peker på en person gjør noe annet. Det avviker fra
anbefalingen i dokumentasjonen, se «Komplett regelsett» under alternativer.

### Kall mot tilgangsmaskinen

- Vi kaller prod-API-et `POST /api/v1/bulk/obo` med saksbehandlerens OBO-token fra
  Texas. Body er `[{ "brukerId": ..., "type": "KJERNE_REGELTYPE" }]`.
- Vi bruker egen klient (`TilgangsmaskinClient`), ikke `tilgangsmaskin-klient` fra
  familie-felles. Vi sjekker én person om gangen, så biblioteket gir lite, og vi
  ser all koden i et sikkerhetskritisk kall.
- Vi bruker bulk-endepunktet og ikke `POST /api/v1/kjerne`. Når forespørselen er
  gyldig, svarer bulk `207` med status per person, så et avslag er data og ikke et
  unntak. En ugyldig forespørsel gir en vanlig feilstatus, for eksempel `413` ved
  mer enn 1000 identer, `400` ved tom `brukerId` og `401` ved ugyldig token. Når
  barn skal sjekkes, kan de tas med i samme kall.
- Bare status `204` gir tilgang. `403`, andre statuser, en ident som mangler i
  svaret og ukjente avvisningskoder gir avslag.
- Tilgangsmaskinen svarer `204` når personen ikke finnes i PDL. Et ukjent
  fødselsnummer gir dermed tilgang. Det gjelder i praksis bare søk på personident,
  fordi de andre identene kommer fra vår egen database, og for en ukjent person
  finnes det ingen persondata å vise.
- Scope er `api://${nais.miljo}-gcp.tilgangsmaskin.populasjonstilgangskontroll/.default`.

### Kontroll i controllerne

- Hver handler kaller `TilgangService.validerTilgangTil…()` som første linje. Det
  finnes én metode per ID-type: personident, `fagsakPersonId`, `fagsakId` og
  `behandlingId`. Alle slår opp aktiv ident og sjekker den.
- En request har én identifiserende ID. Alt annet utledes fra den.
- PDF-endepunktet for journalposter tar `fagsakPersonId` som query-parameter. Vi
  sjekker tilgang til personen og at journalposten og dokumentet tilhører personens
  journalposter i SAF.
- Under-ressurser, som vilkårsperioder, slås opp med forelder-ID.
  `VilkårPeriodeService.hentPeriodePåBehandling` gir 404 når perioden tilhører en
  annen behandling.
- `EndepunktRegisterTest` lister handlerne i pakken `no.nav.grunn.og.hjelpestonad`
  med profilen `dev`. Et nytt endepunkt der får testen til å feile til det er
  plassert som «med personsjekk» eller «uten personsjekk». Testen ser ikke
  task-endepunktene fra prosessering-biblioteket eller controllere som bare finnes
  i andre profiler.

Endepunkter uten personsjekk: `DiagnosekodeController`, `UnleashController`,
`TestDbController` og task-endepunktene fra prosessering (`/api/task/**`), som
`etterlatte-prosessering` bruker.

### Svar ved avslag og feil

- Avslag gir `403` med en melding per avvisningskode, for eksempel «Du har ikke
  tilgang til brukere med strengt fortrolig adresse (kode 6).». Ukjent eller
  manglende kode gir «Du har ikke tilgang til denne personen». Saksbehandleren
  trenger grunnen for å sende saken til riktig enhet. Ulempen er at meldingen
  avslører egenskaper ved personen til en saksbehandler uten tilgang, se Risiko.
- Når tilgangsmaskinen ikke svarer, svarer med feilstatus eller svarer uten body,
  nekter vi tilgang og svarer `503` med «Vi kunne ikke sjekke om du har tilgang.
  Prøv igjen om litt.». Avslag (`ManglerTilgangTilPersonFeil`, `403`) og tekniske feil
  (`TilgangsmaskinException`, `503`) er to ulike exceptions, så det ene ikke kan
  bli til det andre.
- Alle avslag som gir `403` arver fra `ManglerTilgang`: `ManglerTilgangTilPersonFeil`,
  `IkkeAnsvarligSaksbehandlerFeil` og `ManglerTilgangIPdlException`. Én handler i
  `ApiExceptionHandler` svarer for alle tre.
- Vi logger NAV-ident, avvisningskode og `traceId`, aldri fødselsnummer.
  `TilgangsmaskinException` har bare statuskode eller exception-type i meldingen,
  og ingen `cause`, fordi responsbodyen kan inneholde fødselsnumre.

### Ingen unntak for maskintokens

`TilgangService` har ingen unntak for systemkall. Et maskintoken gir `403` før
kallet til tilgangsmaskinen. `SikkerhetContext.erMaskinTilMaskinToken()` gir
`true` når `NAVident` mangler, også uten autentisering. Den brukes derfor bare til
å nekte tilgang, aldri til å hoppe over sjekken.

## Alternativer vurdert

### `tilgangsmaskin-klient` fra familie-felles

Biblioteket deler opp kall over 1000 identer, nekter tilgang ved manglende svar og
holder fødselsnumre unna feilmeldingene. Vi trenger ikke oppdelingen, og de to
andre er få linjer å skrive selv. Forkastet for å unngå avhengighet til et annet
teams utgivelser i et sikkerhetskritisk kall.

### Komplett regelsett

Dokumentasjonen anbefaler komplett regelsett ved oppslag på hovedparten. Det
sjekker i tillegg geografisk tilgang og avdød i mer enn 12 måneder. Vi trenger
ingen av de to reglene:

- **Geografi:** Grunn- og hjelpestønad behandles nasjonalt, ikke etter hvor
  brukeren bor. Med komplett regelsett måtte alle saksbehandlerne hatt
  `0000-GA-GEO_NASJONAL`. Det gir samme resultat som å droppe regelen, men hver
  saksbehandler må bestille tilgangen i Mine Tilganger.
- **Avdød:** Saker kan fortsette etter at brukeren er død, for eksempel ved
  tilbakekreving eller etterbetaling til dødsboet. Regelen ville stoppet
  saksbehandlere uten `0000-GA-Avdod_Bruker` fra å avslutte slike saker.

Forkastet.

### Sjekk via AOP eller annotasjoner

Mindre kode, men det blir vanskeligere å se hva som sjekkes og mot hvilken ID.
Forkastet. Kall i controllerne følger mønsteret fra familie-appene, og
auditloggen kan senere legges på samme sted.

### Logge avvik før tilgangsmaskinen blir master

Dokumentasjonen anbefaler en periode der teamet logger avvik mot egne
beslutninger. Forkastet fordi appen ikke er i prod og ikke har egen tilgangslogikk
å sammenligne med.

## Sikkerhet og personvern

- Tilgangsmaskinen får fødselsnummeret til stønadsmottaker og NAV-ident fra
  OBO-tokenet. Det er en etablert flyt i Nav.
- Testendepunktene `/api/tilgangsmaskin/*` er fjernet. `/api/tilgangsmaskin/ansatt`
  ga alle innloggede fødselsnummeret til en vilkårlig ansatt og familien hens.
- Den døde `permitAll`-regelen for `/api/test/infotrygd/uautentisert` er fjernet.

## Konsekvenser

### Positive

- Vi bruker samme regler for strengt fortrolig og fortrolig adresse, skjerming,
  habilitet og verge som andre fagsystemer i Nav.
- Saksbehandlerne trenger ikke geografiske grupper.
- Nye kjerneregler i tilgangsmaskinen gjelder for oss uten kodeendring.

### Negative

- Hvert API-kall gir ett ekstra synkront kall til tilgangsmaskinen. Vi cacher ikke
  selv; tilgangsmaskinen cacher grupper i 3 timer og persondata i 12 timer.
- Når tilgangsmaskinen er nede, kan ingen saksbehandle.

### Risiko

- Tilgangsmaskinen skriver at det sannsynligvis kommer krav i 2026–2027 om å
  sjekke tilgang til avdøde brukere. Da må vi bytte til komplett regelsett eller
  sjekke avdød på annen måte.
- Avslagsmeldingen viser hvilken kjerneregel som avviste. En saksbehandler kan
  dermed finne ut om et fødselsnummer har strengt fortrolig eller fortrolig
  adresse, eller er skjermet. Avslag logges med NAV-ident, men ingen varsles.
- Ingen auditlogg ennå. Teamet har valgt å vente med auditlogg og godtar risikoen
  til den er på plass.
- Diagnosekoder fra Infotrygd (`POST /api/infotrygd/diagnose` i infotrygd og i
  sak) er helseopplysninger etter GDPR art. 9. Oppslagene auditlogges ikke.
  `AuditLogger` i sak finnes, men ingen kode kaller den. Kjerneregelsettet
  gjelder også for diagnosekodene, fordi reglene vi har valgt bort (geografi og
  avdød), ikke handler om hvor sensitive dataene er.
- Infotrygd-fanen viser diagnosekodene for saksbehandler i dev, uten auditlogg.
  Infotrygd i dev har skarpe data. Teamet godtar risikoen i dev, men ikke i prod.
- `LESETILGANG` får se diagnosekodene, på samme måte som `SAKSBEHANDLER` og
  `BESLUTTER`. Det er et bevisst valg. Svaret fra sak har ikke med
  personidenten.

## Aksjonspunkter

- [ ] Test i dev med testbrukere: én med kode 6 og én skjermet.
- [ ] Prod-manifest: outbound-regel til `populasjonstilgangskontroll` i namespace
      `tilgangsmaskin`.
- [ ] Sjekk tilgangskontrollen på `/api/task/**`. `ApplicationConfig` leser
      `prosessering.rolle`, men `harTilgang()` gir alltid `true`.
- [ ] Sjekk barn når barn blir en del av saken.
- [ ] Vurder regelsettet på nytt når Nav stiller krav om å sjekke avdøde brukere.
- [ ] Auditlogg: database for økonominære handlinger og ArcSight via `AuditLogger`.
      Oppslag av diagnosekoder fra Infotrygd må auditlogges før de vises for
      saksbehandler i prod. Det blokkerer prod-manifestet for sak.
