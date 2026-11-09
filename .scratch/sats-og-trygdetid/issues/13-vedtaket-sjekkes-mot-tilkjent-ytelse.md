# 13: Vedtaket sjekkes mot tilkjent ytelse

**What to build:** Saksbehandler velger fortsatt resultatet i vedtaket, men i en førstegangsbehandling stopper backend et resultat som ikke stemmer med tilkjent ytelse. Feltene fra barnetilsyn er borte fra vedtaket. Se ADR-0014 under «Vedtaket sjekkes mot tilkjent ytelse».

**Blocked by:** 10

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Beløpsperiodene fra barnetilsyn, med utgifter og barn, er fjernet fra vedtaket, API-et og frontend. Kode som bare de brukte, er slettet.
- [ ] I en førstegangsbehandling gir `INNVILGET` uten noen måned med rett en funksjonell feil.
- [ ] I en førstegangsbehandling gir `AVSLÅTT` med minst én måned med rett en funksjonell feil.
- [ ] En måned med 0 kr på grunn av trygdetid 0/40 regnes som en måned med rett.
- [ ] Revurderinger sjekkes ikke ennå. En kommentar viser til spec-en.
- [ ] Testene for vedtak dekker de fire kombinasjonene av resultat og tilkjent ytelse i en førstegangsbehandling.
- [ ] API-klienten er generert på nytt, og `npx tsc` passerer.
