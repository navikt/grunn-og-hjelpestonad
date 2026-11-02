# 09: Utledning av tilkjent ytelse etter § 22-12

**What to build:** En ren funksjon gjør vilkårsradene og satsbeløpene om til tilkjent ytelse per måned, eller gir en liste med valideringsfeil. Funksjonen er kjernen i ADR-0014 og følger reglene i [utledning av tilkjent ytelse](../../../docs/tilkjent-ytelse/utledning-av-tilkjent-ytelse.md). Ticket 10 kobler den til steg 2.

🔴 Rød sone: kjernelogikk for hva søker får utbetalt. Gå gjennom reglene og testene nøye.

**Blocked by:** 04, 07, 08

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Funksjonen er ren Kotlin uten Spring, etter ADR-0008. Inn går vilkårsradene for medlemskap, diagnose, ekstrautgifter og trygdetid og listen over satsbeløp. Ut går andeler per måned, eller en valideringsfeil med én feil per vilkårstype.
- [ ] Validering: finnes det perioder med oppfylte inngangsvilkår, må minst én ekstrautgiftsperiode være vurdert. Trygdetiden må dekke alle tidsrom der ekstrautgiftene er `JA` innenfor periodene med oppfylte inngangsvilkår.
- [ ] Søker har rett der inngangsvilkårene er oppfylt og ekstrautgiftene er `JA`. Hull i ekstrautgiftene betyr «nei».
- [ ] Perioder for ekstrautgifter og trygdetid utenfor periodene med oppfylte inngangsvilkår påvirker ikke resultatet og gir ingen feil.
- [ ] § 22-12 første ledd: retten gjelder fra måneden etter den måneden den oppstår, også når den oppstår den 1.
- [ ] § 22-12 sjette ledd: utbetalingen stopper ved utgangen av måneden retten faller bort.
- [ ] § 22-12 fjerde og femte ledd: høyere sats eller brøk gjelder fra samme måned, lavere fra måneden etter. Hver endring vurderes for seg.
- [ ] Satsbeløpet følger datoene i listen, så en satsendring fra Stortinget midt i en periode gir nytt beløp fra den måneden.
- [ ] Beløpet er satsbeløpet × teller / 40 som `BigDecimal`, eksakt uten avrunding.
- [ ] En måned med 0 kr gir ingen andel. Måneder med likt beløp slås sammen til én andel.
- [ ] KDoc viser til ledd i § 22-12 og sier at tolkningen ikke er bekreftet av fag.
- [ ] Eksempelet i dokumentet om utledning av tilkjent ytelse er en test.
- [ ] Det er én test per regel over, og én test der økning og reduksjon skjer i samme måned.
- [ ] Utledningen av perioder med oppfylte inngangsvilkår fra ticket 05 brukes, ikke kopieres.
