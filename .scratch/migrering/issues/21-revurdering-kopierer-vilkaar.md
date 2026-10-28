# 21: Revurdering kopierer vilkår fra forrige behandling

**What to build:** En revurdering starter med vilkårene fra forrige iverksatte behandling, som `VurderingService.kopierVurderingerOgSamværsavtalerTilNyBehandling` i EF. I en migrert sak kopieres vilkårene som ble opprettet maskinelt i migreringen.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

I dag lagres vilkårsperiodene per behandling, og `forrigeBehandlingId` brukes ikke når en revurdering opprettes. En revurdering starter derfor uten vilkår. Endringen gjelder alle revurderinger, ikke bare migrerte saker. Teamet bestemte 2026-10-28 å gjøre som EF.

Slik gjør EF det (`RevurderingService.opprettRevurdering`):

- Alle vilkårsvurderinger fra forrige behandling kopieres med ny ID og ny behandlings-ID. Resultat og delvilkår følger med.
- Hver kopi får `opphavsvilkår`, som peker på behandlingen vurderingen først ble gjort i.
- Revurderingen stoppes hvis forrige behandling ikke har vilkår.
- Maskinelt oppfylte vilkår fra migreringen kopieres som andre vilkår.

Krav:

- [x] Vilkårsperiodene for medlemskap, diagnose og institusjon fra forrige iverksatte behandling kopieres til revurderingen når den opprettes, med ny ID, ny behandlings-ID og ny `sporbar`.
- [x] Perioder med rett kopieres ikke. De utledes når vilkårssteget fullføres.
- [x] Saksbehandler kan endre de kopierte vilkårene som andre vilkår.
- [x] Revurderingen stoppes med en feil hvis forrige behandling ikke har vilkår.
- [x] Tester dekker: førstegangsbehandling uten kopiering, revurdering med vilkår fra forrige behandling og forrige behandling uten vilkår.
- [x] Opphav og merket «maskinelt opprettet» krever nye kolonner og er flyttet til ticket 25.

## Comments

**2026-10-28:** Gjort i `KopierVilkårService`, som `OpprettBehandlingService` kaller når det finnes en forrige iverksatte behandling. Hele testsuiten i `apps/sak` er grønn (376 tester), og ktlint er grønn. Merk: en revurdering i dev feiler nå hvis forrige iverksatte behandling ble laget før vilkårene fantes.
