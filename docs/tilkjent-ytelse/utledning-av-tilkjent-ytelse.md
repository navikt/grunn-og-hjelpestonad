# Utledning av tilkjent ytelse

Tilkjent ytelse er satsbeløpet ganget med trygdetidsbrøken, per måned og uten
avrunding. Hvilke måneder søker får utbetalt for, og fra hvilken måned et nytt
beløp gjelder, følger av folketrygdloven § 22-12. Avrundingen til hele kroner
følger av § 22-11 og skjer i koden for oppdrag.

Begrepene er definert i [CONTEXT.md](../../CONTEXT.md). Vilkårene er beskrevet i
[vilkår.md](../vilkår/vilkår.md).

## Fra vilkår til tilkjent ytelse

1. Søker har rett i et tidsrom når medlemskap og diagnose er oppfylt, og
   nødvendige ekstrautgifter er vurdert til ja. Et tidsrom uten vurdering
   regnes som ikke oppfylt.
2. Hvert tidsrom med rett har en sats og en trygdetidsbrøk.
3. § 22-12 gjør tidsrommene om til måneder, se under.
4. Tilkjent ytelse for en måned er satsbeløpet for måneden × trygdetid / 40.
   Satsbeløpet er månedsbeløpet fra
   [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/v1-06-03).
5. En måned med trygdetid 0/40 gir 0 kr. Den gir ingen andel, men er en måned
   med rett i vedtaket og i brevet.

Satsbeløpet er et helt kronebeløp, og 1/40 er 0,025. Tilkjent ytelse har derfor
aldri mer enn tre desimaler, og vi lagrer den eksakt.

## § 22-12: når utbetalingen starter, endres og stopper

Grunnstønad gis per måned, jf. § 22-10 tredje ledd. Da gjelder disse reglene i
[§ 22-12](https://lovdata.no/nav/lov/1997-02-28-19/§22-12):

| Hendelse | Gjelder fra | Ledd |
|---|---|---|
| Retten oppstår | Måneden etter den måneden vilkårene er oppfylt | Første |
| Ytelsen settes opp: høyere sats eller høyere trygdetidsbrøk | Samme måned som vilkårene for økningen er oppfylt | Fjerde |
| Ytelsen settes ned: lavere sats eller lavere trygdetidsbrøk | Måneden etter den måneden vilkårene for reduksjonen er oppfylt | Femte |
| Retten faller bort | Utbetalingen stanses ved utgangen av måneden | Sjette |

Regelen om økning og reduksjon gjelder hver endring for seg. Øker satsen og
synker trygdetidsbrøken i samme måned, gjelder den nye satsen fra samme måned
og den nye brøken fra måneden etter.

Når Stortinget endrer satsbeløpet, gjelder det nye beløpet fra datoen
Stortinget har satt. Det er ikke en endring etter § 22-12, men et nytt beløp
for samme sats.

### Eksempel

Inngangsvilkårene er oppfylt fra 1. mars. Satsen er 2 fram til 14. juni og 4
fra 15. juni. Søker får alderspensjon med trygdetid 30/40 fra 10. september.

| Måned | Sats | Trygdetidsbrøk | Hvorfor |
|---|---|---|---|
| Mars | Ingen | Ingen | Retten oppstår i mars og gjelder fra måneden etter |
| April og mai | 2 | 40/40 | |
| Juni til september | 4 | 40/40 | Høyere sats gjelder fra samme måned |
| Fra oktober | 4 | 30/40 | Lavere brøk gjelder fra måneden etter |

### Ikke bekreftet av fag

Vi leser § 22-12 bokstavelig. Det betyr at retten gjelder fra måneden etter også
når vilkårene er oppfylt den 1. i en måned. Fag må bekrefte at dette stemmer
med praksis. Fram til da er regelen merket som ubekreftet i koden.

## Migrering og § 22-12

I en migrering overtar ny løsning utbetalingen fra startmåned. Migreringen
setter vilkårene til oppfylt, og den første revurderingen kopierer dem og regner
ut tilkjent ytelse på nytt med reglene over.

Starter vilkårene i startmåned, gir § 22-12 rett først fra måneden etter.
Revurderingen mister da startmåned og opphører en måned ny løsning allerede har
utbetalt for. Det gir en feilutbetaling uten at noe i saken er endret.

Vilkårene i migreringen må derfor starte i måneden før startmåned. Da gir
§ 22-12 rett fra og med startmåned, og en revurdering uten endringer gir samme
tilkjent ytelse som migreringen.

Vilkårene kan heller ikke starte tidligere, for eksempel der grunnstønaden
startet i Infotrygd. Da gir revurderingen andeler før startmåned, startmåned
flyttes bakover, og ny løsning utbetaler for måneder Infotrygd allerede har
utbetalt for. Å flytte startmåned bakover skal være et bevisst valg i en
revurdering, ikke en bivirkning av migreringen.

Kravet står i ticket 16 og testes i ticket 23 under `.scratch/migrering/issues/`.

## § 22-11: avrunding

[§ 22-11](https://lovdata.no/nav/lov/1997-02-28-19/§22-11) sier at
terminbeløp avrundes til nærmeste hele krone. Saken lagrer tilkjent ytelse med
desimaler. Oppdrag avrunder til utbetalingsbeløpet, se
[ADR-0011](../adr/ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md).

Loven sier ikke hva som skjer med nøyaktig 50 øre. Det skjer i praksis: sats 2
fra 2024 er 1 146 kr, og 1 146 × 10/40 = 286,50 kr. Til fag har bestemt noe
annet, runder oppdrag opp (`HALF_UP`).
