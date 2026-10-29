# 20: «Migrer» i frontend

**What to build:** Saksbehandler ser på personen om saken kan migreres, perioden og beløpene, og kan migrere med én knapp. Kan saken ikke migreres, vises årsaken.

**Blocked by:** 14, 16

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Forhåndsvisningen henter `GET /api/migrering/{fagsakPersonId}` med den genererte API-klienten.
- [ ] Knappen kaller `POST` og er deaktivert mens kallet pågår.
- [ ] Feil fra kontroll 1 og simuleringen vises med måned og beløp.
- [ ] Etter migreringen går saksbehandler til behandlingen.
- [ ] Komponentene bruker Aksel (aksel-builder).
- [ ] Teksten er vasket av forfatter.
- [ ] Knappen vises bare når feature toggle for migrering er på.
