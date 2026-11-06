# 18: Hjemmelbokstav og diagnoser i Infotrygd

**What to build:** Målinger fra Infotrygd-replikaen i dev som viser hvilke verdier hjemmelbokstav, diagnose og sats har i løpende saker. Ticket 15 trenger tallene for å avgjøre hvordan en migrert sak blir til den nye modellen.

**Blocked by:** None (can start immediately)

**Status:** needs-info

Bakgrunn: [README for `apps/infotrygd`](../../../apps/infotrygd/README.md#evaluering). Evalueringen har `diagnose1` (posisjon 055), `diagnose2` (100), `sats` (150) og `hjemmelbokstav` (160). Diagnosene og hjemmelbokstaven er helseopplysninger. Skriv bare antall i denne fila, aldri fødselsnummer eller enkeltsaker.

- [ ] Tallene under «Spørsmål» er målt og skrevet under `## Answer`.
- [ ] README for `apps/infotrygd` er oppdatert med verdiene hjemmelbokstav kan ha.

## Spørsmål

1. Hvilke verdier har `hjemmelbokstav` i løpende saker, og hvor mange saker har hver verdi? Står det alltid én bokstav, eller kan det stå flere?
2. Finnes det løpende saker med bokstav d eller e? De ble fjernet fra § 6-3 for nye tilfeller fra 1. januar 2020, så eldre saker kan ha dem.
3. Hvor mange løpende saker har én diagnose, to diagnoser eller ingen?
4. Hvor mange løpende saker mangler hjemmelbokstav eller sats, eller har motstridende verdier?
5. Stemmer satsen i evalueringen med `sats_gs` i `t_gh` for samme vedtak?

## Comments
