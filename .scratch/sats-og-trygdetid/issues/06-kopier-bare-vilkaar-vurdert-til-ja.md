# 06: Ny behandling kopierer bare vilkår vurdert til ja

**What to build:** En ny behandling starter med periodene vurdert til ja fra forrige iverksatte behandling, som i barnetrygd. Perioder vurdert til nei blir hull, og saksbehandler vurderer dem på nytt. Kildene fra familie-ba-sak står i [vilkår.md](../../../docs/vilkår/vilkår.md#kopiering-til-ny-behandling).

**Blocked by:** 01

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Medlemskap og diagnose kopierer bare rader med `JA`.
- [ ] Kopieringen kaster ikke feil når ingenting blir kopiert, så en revurdering etter et avslag kan starte uten vilkår.
- [ ] Kopieringen er bygget slik at ticket 07 kan legge til ekstrautgifter med samme regel, og ticket 08 kan legge til trygdetid med alle rader.
- [ ] Testene for kopiering dekker `JA`, `NEI`, en blanding av dem og ingen rader.
