# 09: Utledning av tilkjent ytelse etter § 22-12

**What to build:** En ren funksjon gjør vilkårsradene, trygdetidsradene og satsbeløpene om til tilkjent ytelse per måned, eller gir en liste med valideringsfeil. Funksjonen er kjernen i ADR-0014 og følger reglene i [utledning av tilkjent ytelse](../../../docs/tilkjent-ytelse/utledning-av-tilkjent-ytelse.md). Ticket 10 kobler den til steg 2.

🔴 Rød sone: kjernelogikk for hva søker får utbetalt. Gå gjennom reglene og testene nøye.

**Blocked by:** 04, 07, 08, 15

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [x] Funksjonen er ren Kotlin uten Spring, etter ADR-0008. Inn går vilkårsradene for medlemskap, diagnose og ekstrautgifter og trygdetidsradene, slik de er lagret. Satsbeløpene leses fra `Satsbeløpene`. Ut går andeler per måned, eller en valideringsfeil med én feil per vilkårstype og én for trygdetid. Feilen for trygdetid har en egen nøkkel, siden `VilkårType` ikke har noen verdi for trygdetid.
- [x] Validering: finnes det perioder med oppfylte inngangsvilkår, må minst én ekstrautgiftsperiode være vurdert. Trygdetiden må dekke alle tidsrom der ekstrautgiftene er `JA` innenfor periodene med oppfylte inngangsvilkår.
- [x] Søker har rett der inngangsvilkårene er oppfylt og ekstrautgiftene er `JA`. Hull i ekstrautgiftene betyr «nei».
- [x] Perioder for ekstrautgifter og trygdetid utenfor periodene med oppfylte inngangsvilkår påvirker ikke resultatet og gir ingen feil.
- [x] § 22-12 første ledd: retten gjelder fra måneden etter den måneden den oppstår, også når den oppstår den 1.
- [x] § 22-12 sjette ledd: utbetalingen stopper ved utgangen av måneden retten faller bort.
- [x] § 22-12 fjerde og femte ledd: høyere sats eller brøk gjelder fra samme måned, lavere fra måneden etter. Hver endring vurderes for seg.
- [x] Satsbeløpet følger datoene i listen, så en satsendring fra Stortinget midt i en periode gir nytt beløp fra den måneden.
- [x] Beløpet er satsbeløpet × teller / 40 som `BigDecimal`, eksakt uten avrunding.
- [x] En måned med 0 kr gir ingen andel. Måneder med likt beløp slås sammen til én andel.
- [x] KDoc viser til ledd i § 22-12 og sier at tolkningen ikke er bekreftet av fag.
- [x] Eksempelet i dokumentet om utledning av tilkjent ytelse er en test.
- [x] Det er én test per regel over, og én test der økning og reduksjon skjer i samme måned.
- [x] Utledningen av perioder med oppfylte inngangsvilkår fra ticket 05 brukes, ikke kopieres.

## Comments

**2026-11-06:** Blokkert av ticket 15. Inndataene om ekstrautgifter, regelen om hull og valideringen over forutsetter `JA`/`NEI` og sats per periode. Det kan endre seg når modellen er valgt.

**2026-11-15:** Implementert i `TilkjentYtelseUtleder`, med tester i `TilkjentYtelseUtlederTest`. ADR-0016 bekreftet `JA`/`NEI` og sats per periode. Valg underveis:

- Retten faller bort i måneden med siste dag med rett. Er siste dag 30. september, er september siste måned med utbetaling.
- Rett uten sluttdato gir siste andel `tom = null`. Ticket 10 gjør `tom` nullable i tilkjent ytelse og oppdrag.
- Er ekstrautgiftene bare vurdert utenfor periodene med oppfylte inngangsvilkår, regnes de som ikke vurdert.
- Fjerde og femte ledd: som forskyvningen av vilkår i ks-sak. `forskyvTilHeleMåneder` flytter retten, satsen og trygdetidsbrøken til hele måneder hver for seg, ut fra om perioden før og etter er en økning eller en reduksjon. Overlapper flyttede perioder i en måned med flere endringer, gjelder den høyeste verdien. En reduksjon den 1. venter også til måneden etter.
- En ny rett som starter i måneden den forrige faller bort, gjelder fra måneden etter, også for satsen. Den første løsningen ga den nye satsen i den måneden.
- Trygdetiden kuttes ikke etter vilkårene. En endring i trygdetiden mellom to perioder med oppfylte vilkår kan derfor endre beløpet i siste måned av den første perioden. Det er et unntak fra punktet om at trygdetid utenfor periodene ikke påvirker resultatet. Fag må bekrefte, se ticket 21.
- `utled(medlemskap, diagnoser, ekstrautgifter, trygdetid)` tar inn radene slik de er lagret. Den kaller `PerioderMedOppfylteInngangsvilkårUtleder` selv, og leser satsbeløpene fra `Satsbeløpene` i stedet for å få listen som parameter.
