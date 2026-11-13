# 18: Beslutter kan lese vilkårene

**What to build:** Beslutter kan hente vurderingene av alle vilkårene i totrinnskontrollen. I dag krever hele kontrolleren for hvert vilkår rollen saksbehandler, også `GET`. Lagring og sletting skal fortsatt kreve saksbehandler.

**Blocked by:** None (can start immediately)

**Status:** needs-triage

Gjelder medlemskap, diagnose og nødvendige ekstrautgifter, og trygdetid når den kommer (ticket 08).

- [ ] `GET` på `/api/behandling/{behandlingId}/vilkar/*` tillater rollen beslutter i tillegg til saksbehandler.
- [ ] `POST` og `DELETE` krever fortsatt saksbehandler.
- [ ] Hver metode kaller fortsatt `validerTilgangTilBehandling`.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert.
- [ ] Beslutter ser vilkårene i lesevisning i frontend.

## Comments
