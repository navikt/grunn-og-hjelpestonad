# 04: Avklar hvordan vilkårene utledes fra Infotrygd

**What to build:** En beslutning om hvordan hvert av de fem vilkårene i en migreringsbehandling fylles fra Infotrygd, og når saksbehandler må fylle inn selv. Ticket 06 bygger på svaret. Dette krever kunnskap om Infotrygd og fagsiden.

**Blocked by:** None (can start immediately)

**Status:** wontfix

Spec: [spec.md](../spec.md). Bakgrunn: ADR-0006, ADR-0010 og avsnittet «Evaluering» i README for `apps/infotrygd`.

- [ ] For medlemskap, diagnose, nødvendige ekstrautgifter, institusjon og trygdetid er det bestemt hvilke felt i Infotrygd som gir vurderingen og periodene.
- [ ] Det er bestemt hva som skjer når et felt står i `motstridendeFelt`, mangler, eller når evalueringen er hentet fra et tidligere vedtak.
- [ ] Det er bestemt hvilke vilkår som alltid må fylles inn av saksbehandler, hvis noen.
- [ ] Det er bestemt hvilken begrunnelse som vises for verdier fra Infotrygd, slik at det ikke ser ut som Nav har vurdert vilkåret i ny løsning.
- [ ] Svaret er skrevet under `## Answer` i denne fila, og `CONTEXT.md` er oppdatert hvis nye begreper oppstår.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.
