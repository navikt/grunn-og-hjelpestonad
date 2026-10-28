# 06: Vilkår fra Infotrygd med kilde og utfylling

**What to build:** Vilkårene i en migreringsbehandling fylles fra Infotrygd. Saksbehandler ser hvilke verdier som kommer fra Infotrygd og hvilke som må fylles inn. Verdier fra Infotrygd er låst. Verdier saksbehandler fyller inn, krever begrunnelse.

**Blocked by:** 02, 04

**Status:** wontfix

Spec: [spec.md](../spec.md)

- [ ] Vilkårene fylles fra `/stonader` og evalueringen etter reglene i svaret på ticket 04.
- [ ] Hver vilkårsverdi i en migreringsbehandling har en kilde: Infotrygd, forrige behandling eller saksbehandler.
- [ ] Verdier saksbehandler fyller inn, lagres med begrunnelse.
- [ ] Skjermbildet låser verdier med kilde Infotrygd eller forrige behandling, og krever begrunnelse for verdier saksbehandler fyller inn.
- [ ] Backend validerer ikke vilkårene i migreringsbehandlinger (ADR-0011).
- [ ] Beslutter ser kilden og begrunnelsen for hver verdi.
- [ ] Vilkårsvurdering i vanlige behandlinger virker som før.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.
