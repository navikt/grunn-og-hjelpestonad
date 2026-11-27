# 28: Tilgang og auditlogg for migreringspersoner med hjemmelbokstav

**Status:** needs-triage

**Blocked by:** None

Spec: [spec.md](../spec.md)

🔴 Rød sone: tilgang til helseopplysninger.

## Uavklart

`GET /api/infotrygd/stonad/migreringspersoner/forstegangsvedtak` returnerer fnr til personer med en gitt sats og **Hjemmelbokstav**. Bokstaven sier noe om helsen, for eksempel F (fordyret kosthold). Endepunktet skal i første omgang ha samme tilgang som `migreringspersoner`:

- Alle rollene (`SAKSBEHANDLER`, `BESLUTTER` og `LESETILGANG`) kan kalle det.
- Tilgangsmaskinen filtrerer bort personer saksbehandleren ikke har tilgang til.
- Endepunktet auditlogges ikke.

Dette må avklares før endepunktet tas i bruk i prod.

## Alternativer

- a) Begrens til `SAKSBEHANDLER`, og skriv én auditlinje per person i svaret med nytt `Oppslag`.
- b) Begrens til `SAKSBEHANDLER` uten auditlogg.
- c) Behold som i dag (valgt foreløpig).

Team logs er ikke gyldig auditlogg i prod, se [ADR-0009](../../../docs/adr/ADR-0009-tilgangskontroll-mot-person.md).
