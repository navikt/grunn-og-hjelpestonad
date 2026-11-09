# 06: Ny behandling kopierer bare vilkår vurdert til ja

**What to build:** En ny behandling starter med periodene vurdert til ja fra forrige iverksatte behandling, som i barnetrygd. Perioder vurdert til nei blir hull, og saksbehandler vurderer dem på nytt. Kildene fra familie-ba-sak står i [vilkår.md](../../../docs/vilkår/vilkår.md#kopiering-til-ny-behandling).

**Blocked by:** 01

**Status:** resolved

Spec: [spec.md](../spec.md)

- [x] Medlemskap og diagnose kopierer bare rader med `JA`.
- [x] Kopieringen kaster ikke feil når ingenting blir kopiert, så en revurdering etter et avslag kan starte uten vilkår.
- [x] Kopieringen er bygget slik at ticket 07 kan legge til ekstrautgifter med samme regel, og ticket 08 kan kopiere alle trygdetidsrader. Trygdetid er ikke et vilkår, så regelen om `JA` gjelder ikke for den.
- [x] Testene for kopiering dekker `JA`, `NEI`, en blanding av dem og ingen rader.

## Comments

Løst med hooken `skalKopieres` i `PeriodeMedBegrunnelseService`. Standarden er `true`, så trygdetid (ticket 08) kopierer alle rader når den bygger på fellesdelen. `VilkårPeriodeService` overstyrer den med `final override` til `erVilkårOppfylt()`, så ekstrautgifter (ticket 07) får regelen om `JA` ved å arve `VilkårPeriodeService`. Ticket 07 må legge ekstrautgiftsservicen inn i listen i `KopierVilkårService`. Ticket 08 kopierer trygdetid for seg, enten med en egen kopieringsservice eller et eget kall fra `OpprettBehandlingService`. Ticket 11 må ikke legge slettingen av tilkjent ytelse i `kopierPerioder`.
