# 26: Gjeldende perioder kuttes ved opphør

**What to build:** Saksbehandler ser gjeldende perioder fra Infotrygd der vedtakene er kuttet ved opphøret av stønaden. Er dataene ikke entydige, får saksbehandler en feilmelding i stedet for perioder. Migreringen bruker de samme periodene senere, så oppslaget viser hva migreringen vil gjøre.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

🔴 Rød sone: reglene for gjeldende perioder. Utvikleren skriver den rene funksjonen og testene. Visningen av feilmeldingen i frontend er grønn sone.

Spec: [spec.md](../spec.md). Beslutningene står i [ticket 03](03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md) og ADR-0012.

- [x] Når stønaden har opphørsdato, kuttes tom på alle vedtakene på stønaden ved opphøret.
- [x] En opphørsdato den 1. i måneden gir tom lik siste dag i måneden før, fordi opphør den 1. er første dag uten stønad. Andre opphørsdatoer brukes som de er. Se «Status» i kommentaren 2026-11-02 i ticket 03.
- [x] Et vedtak som starter samme dag som opphøret, er opphørt fra start. Det tas ikke med og gir ingen feil.
- [x] Kuttet skjer før vedtakene legges på tidslinjen, så det nyeste vedtaket fortsatt vinner og et eldre vedtak gjelder igjen etter at et nyere slutter.
- [x] Et vedtak som får tom før fom etter kuttet, gir `UgyldigInput` med teksten «Vedtak {vedtakId} starter {fom}, etter at stønaden opphørte {opphørsdato}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.»
- [x] Har minst to stønader vedtak uten tom etter kuttet, gir oppslaget `UgyldigInput` med teksten «Personen har løpende vedtak på flere stønader i Infotrygd ({stonadId-er}). Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.»
- [x] Vedtak som er ugyldige uten kuttet, gir samme feil som i dag.
- [x] Er personens siste vedtak avsluttet på eller etter 2026-01-01, mens stønaden ikke er opphørt, gir oppslaget `UgyldigInput` med teksten «Stønad {stonadId} er ikke opphørt, men siste vedtak {vedtakId} sluttet {tom}. Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.» Eldre stønader som ikke er opphørt, regnes som avsluttet.
- [x] Feilmeldingene har ikke fnr.
- [x] Frontend viser feilmeldingen der tabellen over gjeldende perioder ellers står.
- [x] Rene tester dekker: opphør midt i et vedtak, opphør den 1. og siste dag i måneden, vedtak som faller bort, løpende vedtak på flere stønader, siste vedtak avsluttet etter datogrensen på en stønad som ikke er opphørt, og at et eldre vedtak på en annen stønad fortsatt gjelder igjen.

## Comments

**2026-11-02:** Frontend og testene er ferdige. Reglene gjenstår og skrives av utvikler.

- Frontend viser varselet under overskriften «Gjeldende perioder», der tabellen ellers står.
- Testene ligger i `GjeldendeInfotrygdPerioderTest`, i `KuttVedOpphør` og `UklareSaker`, og er `@Disabled` til reglene er på plass. Med `@Disabled` slått av feiler 11 av 16 mot dagens kode. De 5 som passerer, er vakttester for tilfeller som ikke skal gi feil eller endring.
- Den gamle testen «datoOpphor på stønaden kutter ikke perioden» er fjernet. «går på tvers av stønader» har fått tom på det eldste vedtaket, ellers ville den gitt feilen om løpende vedtak på flere stønader.
- `utled` har en TODO som peker på testene.

Antakelser i testene, som utvikleren bør bekrefte:

- Datoene i meldingene har formatet `dd.MM.yyyy`, som feilen i `validerPeriode`.
- Stønads-ID-ene står stigende og skilt med komma: «(10, 20)».
- «Personens siste vedtak» er vedtaket med høyest `vedtakId`, som ellers i ADR-0012.
- Opphørsdatoen i meldingen er `datoOpphor` fra Infotrygd.

Åpent: et vedtak der tom blir lik fom etter kuttet, er ikke dekket. Med regelen fra EF skjer det når et vedtak starter siste dag i måneden før et opphør den 1. Det er trolig sjeldent, siden vedtak som regel starter den 1. `validerPeriode` avviser tom lik fom for vedtak uten kutt.

`@forfatter` foreslo endringer i tekstene. Testene bruker fortsatt teksten fra ADR-0012. Endres tekstene, må ADR-0012, ticket 03 og testene endres sammen:

- Siste setning, «Vi har ikke bestemt hvordan slike vedtak/saker skal tolkes», blir «Vi har ikke bestemt hvordan vi skal tolke dette» i alle tre meldingene.
- «({stonadId-er})» blir «(stønad {stonadId-er})», så det er tydelig hva tallene er.

**2026-11-02:** Hva opphør den 1. betyr, er under avklaring. Se «Status» i kommentaren 2026-11-02 i ticket 03. Endringer i testene:

- «opphør den 1 i måneden gir tom siste dag i måneden før» er `@Disabled` til det er avklart. Varer stønaden ut opphørsmåneden, skal testen forvente 2021-06-30.
- Tre tester som brukte opphør den 1. uten å teste den regelen, bruker nå siste dag i måneden. De passer med begge reglene.
- Det mangler en test for vedtak som er opphørt fra start, altså vedtak som starter samme dag som opphøret.

**2026-11-02:** Saksbehandler har bekreftet at opphør den 1. er første dag uten stønad, som i EF. Se ticket 03.

- «opphør den 1 i måneden gir tom siste dag i måneden før» forventer 2021-05-31, som er riktig. Den er `@Disabled` til regelen er på plass, som de andre testene for ticket 26.
- De tre testene som ble endret til siste dag i måneden, kan stå som de er.

**2026-11-03:** Alle testene i `GjeldendeInfotrygdPerioderTest` er slått på og går gjennom.

- Vedtak som er ugyldige i Infotrygd, valideres før kuttet, så meldingen viser datoene fra Infotrygd.
- Nye tester for vedtak som er opphørt fra start, både med opphør midt i måneden og den 1.
- Fortsatt åpent: tom lik fom etter kuttet (vedtak som starter siste dag i måneden før et opphør den 1.) gir feilen «starter …, etter at stønaden opphørte …». Den meldingen stemmer ikke for dette tilfellet.
