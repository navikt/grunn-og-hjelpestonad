# 03: Avklar perioder og utbetalt beløp fra Infotrygd

**What to build:** En beslutning om hvordan en migreringsbehandling får perioder og utbetalt beløp per måned fra Infotrygd. Ticket 14 og 15 bygger på svaret. Dette krever kunnskap om Infotrygd og fagsiden, og kan ikke avgjøres av en agent alene.

**Blocked by:** None (can start immediately)

**Status:** resolved

Spec: [spec.md](../spec.md). Bakgrunn: ADR-0005 (risiko og aksjonspunkter) og README for `apps/infotrygd`.

- [x] Det er bestemt hvilke delytelser som gir perioder for grunnstønad, og hvordan overlappende delytelser og flere endringskoder per vedtak tolkes.
- [x] Det er bestemt hvordan utbetalt beløp per måned regnes ut, som kontroll 1 sammenligner mot.
- [x] Det er bekreftet om regelen om `oppdrag_id` fra EF gjelder for grunnstønad.
- [x] Det er bestemt hvilken registrering i `t_gh` som gir sats og trygdetidsbrøk for en periode.
- [x] Svaret er skrevet under `## Answer` i denne fila, og README for `apps/infotrygd` er oppdatert hvis svaret endrer det som står der.

## Comments

**2026-10-28:** Gjelder fortsatt etter ADR-0013. Ticket 14 og 15 bygger på svaret i stedet for ticket 05. Valideringen i ticket 14 trenger i tillegg «summerte» perioder som i EF: én periode per sammenhengende tidsrom med månedsbeløp, fom og tom. `GjeldendeInfotrygdPerioder.utled` (ADR-0012, arv og gjeldende perioder) gir allerede hvilket vedtak som gjelder når, og er trolig utgangspunktet. Avklar også om grunnstønad har noe som tilsvarer åpen sak i `InfotrygdService.hentSaker` i EF.

**2026-10-30:** Avklart i en grilling. Tallene under er målt i dev samme dag. Reglene for perioder bygges i ticket 26 og 27, som 14 og 15 nå er blokkert av.

## Answer

### Én kilde til perioder

Gjeldende perioder fra ADR-0012 er den eneste kilden til perioder. Migreringen og «Gjeldende perioder» i frontend bruker de samme reglene, slik at saksbehandler ser i oppslaget hva migreringen vil gjøre. Migreringen legger bare til sjekker av den siste perioden. Sjekkene endrer ingen perioder.

`apps/infotrygd` gir rådata. En ren funksjon i `sak` lager de gjeldende periodene og slår dem sammen. Vi lager ikke et endepunkt for sammenslåtte perioder i `apps/infotrygd`, slik EF-appen har.

Når dataene er uklare, gir vi heller én feil for mye. Det er lettere å endre reglene for når en migrering stopper, enn å rette en migrering som har gått galt.

### Perioder

- Vedtaket avgjør perioden, fra `dato_innv_fom` til `dato_innv_tom`, kuttet ved opphør. Delytelsen flytter aldri fom eller tom, men et avvik mellom delytelsen og vedtaket kan gi feil, se listen under.
- Når stønaden er opphørt, kuttes tom på alle vedtakene på stønaden ved `dato_opphor`. Er opphørsdatoen den 1. i måneden, blir tom siste dag i måneden før, som i EF. Andre datoer brukes som de er. I dev er 98 672 opphørsdatoer siste dag i en måned og 2 031 den 1.
- Det nyeste vedtaket (høyest `vedtakId`) vinner, også på tvers av stønader. Et eldre vedtak gjelder igjen etter at et nyere slutter, som i ADR-0012. EF kutter i stedet alt etter fom på det nyere vedtaket. Forskjellen gjelder 5 stønader i dev, alle med siste vedtak mellom 2009 og 2020. En Infotrygd-ekspert bør bekrefte regelen.
- Vedtak der alle endringskodene er `AN` (Annullert) eller `UA` (Uaktuell), er ikke med. `/stonader` filtrerer dem allerede bort.
- Er stønaden ikke opphørt, men alle vedtakene er avsluttet, er saken avsluttet. Unntaket er når personens siste vedtak er på en slik stønad og slutter på eller etter datogrensen 2026-01-01. Da gir oppslaget og migreringen feil, se listen under. I dev gjelder det 427 stønader hos 419 personer, og bare 1 person får feil.
- Perioder som følger etter hverandre, slås sammen når Infotrygd-beløp, sats, trygdetid, diagnose og hjemmelbokstav er like.

Disse tilfellene gir feil, både i oppslaget og i migreringen. Vi vet ennå ikke hva som er riktig tolkning, og ser på dem når vi møter dem:

- **Et vedtak får tom før fom etter kuttet ved opphør.** 2 122 vedtak i dev, hos 398 av 147 482 personer som kan migreres. Ingen er opphørsvedtak: de har resultat `FI` eller `I` og beløp over 0. De fleste er satsomregninger (`MS`/`FI`) laget før stønaden ble opphørt bakover i tid. Melding: «Vedtak {vedtakId} starter {fom}, etter at stønaden opphørte {opphørsdato}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.»
- **Minst to stønader har vedtak uten tom etter kuttet.** 2 personer i dev. Melding: «Personen har løpende vedtak på flere stønader i Infotrygd ({stonadId-er}). Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.»
- **Personens siste vedtak er avsluttet på eller etter datogrensen, men stønaden er ikke opphørt.** 1 person i dev. Saken kan løpe i Infotrygd selv om vedtaket er avsluttet, og da ville migreringen tatt feil. Melding: «Stønad {stonadId} er ikke opphørt, men siste vedtak {vedtakId} sluttet {tom}. Vi har ikke bestemt hvordan slike saker skal tolkes. Kontakt teamet.»
- **Delytelsen dekker mindre enn vedtaksperioden etter kuttet**, i hvilken som helst periode. 78 vedtak hos 77 personer i dev, og 76 av dem kan migreres. Alle er løpende vedtak der delytelsen sluttet tidligere.
- **Delytelsen har en annen periode enn vedtaket i den siste gjeldende perioden**, også når den dekker mer. I dev gjelder det 1 942 personer som kan migreres, alle med delytelse som slutter senere enn vedtaket. Avvik i tidligere perioder gir ikke feil, og vedtaket avgjør perioden.

De to feilene om delytelser har samme melding: «Vedtak {vedtakId} gjelder {fom}–{tom}, men delytelsen med beløpet i Infotrygd gjelder {delytelseFom}–{delytelseTom}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.» Er vedtaket eller delytelsen løpende, står det «fra {fom}» i stedet for perioden. Meldingen nevner delytelsen fordi den er til både saksbehandler og teamet som forvalter appen.

### Infotrygd-beløp

- Infotrygd-beløpet kommer fra grunnstønad-delytelsen på vedtaket. Alle vedtak på grunnstønad unntatt 2 har nøyaktig én slik delytelse, og ingen vedtak har delytelser med ulikt beløp. Alle beløpene er hele kroner.
- Infotrygd-beløpet er det Oppdrag utbetaler, til ny løsning endrer det. Kontroll 1 sammenligner utbetalingsbeløpet per måned med Infotrygd-beløpet.
- Delytelsen har ofte en annen periode enn vedtaket. Uten kuttet ved opphør gjelder det 7 726 vedtak i dev. Med kuttet gjelder det 98 196 vedtak der delytelsen slutter senere, og 12 der den starter tidligere. Kuttet forkorter vedtaket, men ikke delytelsen, og det forklarer trolig det meste av økningen. I tidligere perioder avgjør vedtaket perioden, og beløpet er kjent for hele perioden.
- Dekker delytelsen mindre enn vedtaksperioden, eller avviker den i den siste perioden, gir både oppslaget og migreringen feil, se «Perioder».

En saksbehandler med tilgang til Infotrygd har sett på to saker. Svarene er ikke fasit for alle saker:

- Et vedtak fra 01.07.2011 til 31.12.2011 hadde en delytelse fra 01.01.2011. Vedtaksbrevet og vedtaksperioden tyder på at vedtaket viser hva som ble utbetalt. Første vedtak var en maskinell satsomregning (`MS`) på samme dato som innvilgelsen. Det hadde saksbehandleren ikke sett før.
- Et løpende vedtak fra 01.11.2025 hadde en delytelse som sluttet 31.12.2025. Stønaden løper fortsatt og utbetales med sats 2. Saksbehandleren fant ingenting i saksbehandlingen som forklarer delytelsen.

Saksbehandlere kjenner ikke ordet «delytelse». Det nærmeste er resultatkoden `DI` (delvis innvilget).

### Sats og trygdetidsbrøk

- Sats og trygdetid kommer fra raden i `t_gh` med nyest `tidspunkt_reg`. Ingen vedtak i dev har rader med ulik trygdetid eller sats.
- Trygdetidsbrøken er trygdetid/40. Trygdetiden ligger mellom 1 og 40, og 29 602 rader har under 40.
- `sats_gs` er 1 til 6, utenom 4 rader med 0.
- Bare 2 vedtak som ikke er annullert, mangler `t_gh`. Begge er løpende, og ingen av dem er maskinvedtak.

### `oppdrag_id`

Regelen fra EF, der perioder uten `oppdrag_id` filtreres bort stille, brukes ikke. Migreringen stopper i stedet med feilmelding når stønaden mangler `oppdrag_id`. I dev gjelder det 7 løpende stønader.

### Åpen sak

En sak i `sa_sak_10` er åpen når `s10_kapittelnr` er `GH`, `s10_valg` er `GS` og `s10_resultat` er tom. Saker med `s10_type` `K`, `KT` eller `A` (klage og anke) regnes ikke med. Regelen kommer fra `InfotrygdSakResultat.ÅPEN_SAK` og `validerFinnesIkkeÅpenSak` i EF, og koden skal ha en kommentar om det.

### Hva migreringen bruker

Migreringen bruker den siste gjeldende perioden. Den går ikke lenger bakover enn til siste vedtak med egen evaluering, som er kilden for arven etter ADR-0012. Har personen ingen periode fremover, migreres siste måned av siste periode med beløp, som i EF.

Migreringen stopper med en egen årsak når:

- personen har en åpen sak i Infotrygd
- vedtaket som gjelder, har endringskode `AN`, `UA`, `O` eller `OO`
- stønaden mangler `oppdrag_id`
- satsen er 0
- beløpet er 0
- vedtaket som gjelder, ikke har egen `t_gh`
- den siste perioden mangler diagnose eller hjemmelbokstav etter arv, eller har motstridende verdier
- det er mer enn én gjeldende periode fremover (`FLERE_AKTIVE_PERIODER`), for eksempel når et nyere vedtak gir ny sats fra neste måned
- siste periode slutter før datogrensen 2026-01-01
- en av feilene under «Perioder» gjelder personen

`FLERE_AKTIVE_PERIODER` gir feil i første omgang. Det kan trolig løses ved å migrere begge periodene, så lenge begge er gyldige.

### Førstegangsbehandling

Ny løsning nekter å opprette den første behandlingen på en fagsak når personen kan migreres etter reglene over. Personen må migreres, slik at saken kan stoppes i Infotrygd. Dette bygges i ticket 19.
