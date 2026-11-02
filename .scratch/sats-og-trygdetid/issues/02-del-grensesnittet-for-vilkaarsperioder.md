# 02: Skill ut felles livsløp for perioder med begrunnelse

**What to build:** Prefactoring uten endret oppførsel. Trygdetid (ticket 08) er ikke et vilkår: den har ingen `JA`/`NEI`, kan ikke gi avslag og reduserer bare tilkjent ytelse etter § 6-6. Den har likevel samme livsløp som vilkårene. Fellesdelen skilles derfor ut fra vilkårsperiodene, under et navn uten «Vilkår», for eksempel `PeriodeMedBegrunnelse`:

- Fellesdelen har id, behandling, begrunnelse, datoer, sporbarhet, tidslinjenøkkel, kopiering, forkorting ved overlapp, redigerbarhet, ansvarlig saksbehandler og endringshistorikk.
- `VilkårPeriode` arver fra fellesdelen og legger til vurderingen og «er oppfylt».

Unngå navnet «behandlingsperiode». Det er allerede brukt i endringshistorikken («Hele behandlingsperioden»).

**Blocked by:** 01

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Fellesdelen har et grensesnitt for perioden, et for requesten og en abstrakt service. Ingen av dem har «Vilkår» i navnet eller kjenner til `Vurdering` eller `VilkårType`.
- [ ] `VilkårPeriode`, `VilkårPeriodeRequest` og `VilkårPeriodeService` bygger på fellesdelen og legger til vurderingen. Medlemskap og diagnose oppfører seg som før.
- [ ] Repositoryet og tidslinjehjelperne som ikke trenger «er oppfylt», krever bare fellesdelen. `erOppfyltTidslinje` krever fortsatt `VilkårPeriode`.
- [ ] Servicen for fellesdelen tar inn hvilke endringstyper den skriver, slik at trygdetid kan få egne typer i ticket 08. Vilkårene bruker fortsatt `VILKÅR_VURDERING_*`.
- [ ] 🔴 Rød sone: fellesdelen bygger detaljene i endringshistorikken av faste felter: type, vurdering hvis perioden har en, og periode. Underklassene kan ikke legge til fritekst. Da kan ingen senere legge diagnosekode eller diagnosebeskrivelse i detaljene. KDoc-en om GDPR artikkel 9 flyttes med.
- [ ] Alle eksisterende tester er grønne uten endrede forventninger. Testen som sjekker at diagnose aldri havner i detaljene, finnes fortsatt.
