# 02: Del grensesnittet for vilkårsperioder

**What to build:** Prefactoring uten endret oppførsel. Trygdetid (ticket 08) skal bruke den felles livsløpet til vilkårene, men har ingen `JA`/`NEI`. Grensesnittet for vilkårsperioder deles derfor i to: fellesfeltene og livsløpet (id, behandling, begrunnelse, datoer, sporbarhet, forkorting ved overlapp, redigerbarhet, ansvarlig saksbehandler, endringshistorikk) på den ene siden, og vurderingen og «er oppfylt» på den andre.

**Blocked by:** 01

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Et vilkår kan bruke den felles servicen for vilkårsperioder uten å ha vurdering.
- [ ] Medlemskap og diagnose bruker begge delene og oppfører seg som før.
- [ ] Tidslinjehjelperne som trenger «er oppfylt», krever bare den delen.
- [ ] Endringshistorikken for vilkår uten vurdering skriver ikke en vurdering. Diagnose havner fortsatt aldri i detaljene.
- [ ] Alle eksisterende tester er grønne uten endrede forventninger.
