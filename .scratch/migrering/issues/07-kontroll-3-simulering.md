# 07: Kontroll 3: simuleringen skal vise 0 kr

**What to build:** En migreringsbehandling kan ikke sendes til beslutter hvis simuleringen fra økonomi viser etterbetaling eller feilutbetaling. Saksbehandler får beskjed om beløpene og kan bare henlegge. Kontroll 1 kjøres også på nytt ved sending til beslutter.

**Blocked by:** 02

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Kontroll 3 er en ren funksjon som får etterbetaling og feilutbetaling fra simuleringsresultatet og returnerer avvik når en av dem er ulik 0 kr.
- [ ] Sending til beslutter stoppes for migreringsbehandlinger når kontroll 3 gir avvik, eller når simuleringen ikke er ferdig.
- [ ] Ved sending til beslutter kjøres også kontroll 1, og kontroll 2 når den finnes, slik at endrede data fanges.
- [ ] Avvik kan ikke overstyres.
- [ ] Rene tester bruker et simuleringsresultat laget i testen og dekker: 0 kr, etterbetaling, feilutbetaling og begge.
- [ ] Sending til beslutter for andre behandlinger virker som før.
