# 09: Revurderinger kan ikke gå før migreringsdatoen

**What to build:** I en migrert sak kan en vanlig revurdering ikke gi vilkårsperioder eller andeler før migreringsdatoen. Saksbehandler får beskjed tidlig i vilkårssteget, og andelssjekken stopper sending til beslutter. Beskjeden sier at måneder før migreringsdatoen må tas over i en «Endre migreringsdato»-behandling. Saker uten migreringsdato påvirkes ikke.

**Blocked by:** 02

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ren funksjon får vilkårsperiodene og gjeldende migreringsdato, og returnerer periodene som starter før datoen. Den brukes når vilkårssteget fullføres.
- [ ] En ren funksjon får andelene og gjeldende migreringsdato, og returnerer andelene som starter før datoen. Den brukes når en behandling sendes til beslutter.
- [ ] Uten migreringsdato returnerer begge funksjonene ingen avvik, så etterbetaling bakover i saker som ikke er migrert virker som før.
- [ ] Feilene er funksjonelle og nevner migreringsdatoen og «Endre migreringsdato»-behandling.
- [ ] Rene tester dekker: ingen migreringsdato, periode i samme måned som datoen, periode før datoen og periode etter datoen.
