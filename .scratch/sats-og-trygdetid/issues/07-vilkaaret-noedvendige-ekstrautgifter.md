# 07: Vilkåret nødvendige ekstrautgifter

**What to build:** Saksbehandler kan vurdere nødvendige ekstrautgifter per periode i steg 2: ja med sats 1–6, eller nei, med begrunnelse. Perioder som ligger utenfor periodene med oppfylte inngangsvilkår, er markert. Se ADR-0006 under «Steg 2 er nødvendige ekstrautgifter og trygdetid».

**Blocked by:** 05, 06

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering oppretter tabellen for vilkåret med fellesfeltene og `sats`. CHECK-constraints krever at `sats` er 1–6 og satt ved `JA`, tom ved `NEI`, og at datoene står i riktig rekkefølge. Tabellen har `ON DELETE CASCADE` fra behandling og indeks på behandling.
- [ ] Vilkårstypen har en ny verdi for nødvendige ekstrautgifter.
- [ ] Servicen bruker den felles servicen for vilkårsperioder, med overlapp, redigerbarhet, ansvarlig saksbehandler og endringshistorikk. Valideringen avviser sats ved `NEI` og manglende sats ved `JA` med 400.
- [ ] Endepunktene ligger på `/api/behandling/{behandlingId}/vilkar/ekstrautgifter`, med hent, lagre og slett, og krever rollen saksbehandler.
- [ ] Kopieringen til ny behandling tar med rader med `JA`.
- [ ] Steg 2 har en liste over ekstrautgiftsperioder, bygget som komponentene i steg 1. Satsvelgeren vises bare ved ja.
- [ ] Perioder utenfor periodene med oppfylte inngangsvilkår er markert i listen.
- [ ] Testene dekker servicen, repositoryet med CHECK-constraints og kopieringen.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.
