# 26: Gjeldende perioder kuttes ved opphør

**What to build:** Saksbehandler ser gjeldende perioder fra Infotrygd der vedtakene er kuttet ved opphøret av stønaden. Er dataene ikke entydige, får saksbehandler en feilmelding i stedet for perioder. Migreringen bruker de samme periodene senere, så oppslaget viser hva migreringen vil gjøre.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

🔴 Rød sone: reglene for gjeldende perioder. Utvikleren skriver den rene funksjonen og testene. Visningen av feilmeldingen i frontend er grønn sone.

Spec: [spec.md](../spec.md). Beslutningene står i [ticket 03](03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md) og ADR-0012.

- [ ] Når stønaden har opphørsdato, kuttes tom på alle vedtakene på stønaden ved opphøret.
- [ ] En opphørsdato den 1. i måneden gir tom lik siste dag i måneden før. Andre opphørsdatoer brukes som de er.
- [ ] Kuttet skjer før vedtakene legges på tidslinjen, så det nyeste vedtaket fortsatt vinner og et eldre vedtak gjelder igjen etter at et nyere slutter.
- [ ] Et vedtak som får tom før fom etter kuttet, gir `UgyldigInput` med teksten «Vedtak {vedtakId} starter {fom}, etter at stønaden opphørte {opphørsdato}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.»
- [ ] Har minst to stønader vedtak uten tom etter kuttet, gir oppslaget `UgyldigInput` med teksten «Personen har løpende vedtak på flere stønader i Infotrygd ({stonadId-er}). Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.»
- [ ] Vedtak som er ugyldige uten kuttet, gir samme feil som i dag.
- [ ] Er personens siste vedtak avsluttet på eller etter 2026-01-01, mens stønaden ikke er opphørt, gir oppslaget `UgyldigInput` med teksten «Stønad {stonadId} er ikke opphørt, men siste vedtak {vedtakId} sluttet {tom}. Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.» Eldre stønader som ikke er opphørt, regnes som avsluttet.
- [ ] Feilmeldingene har ikke fnr.
- [ ] Frontend viser feilmeldingen der tabellen over gjeldende perioder ellers står.
- [ ] Rene tester dekker: opphør midt i et vedtak, opphør den 1. og siste dag i måneden, vedtak som faller bort, løpende vedtak på flere stønader, siste vedtak avsluttet etter datogrensen på en stønad som ikke er opphørt, og at et eldre vedtak på en annen stønad fortsatt gjelder igjen.
- [ ] Teksten i feilmeldingene er gjennomgått av `@forfatter`.
