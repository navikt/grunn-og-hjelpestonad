# ADR-0010: Historiske evalueringsdata fra Infotrygd kobles på person og måned

**Dato:** 2026-10-20
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`/api/infotrygd/stonader` viser siste evaluering for saken hvert vedtak hører
til. Evalueringen ligger i to tabeller i Infotrygd-replikaen:

- `ev_valg_05` har én rad per evaluering, med person (`ev01_personkey`), måned
  (`ev05_aar_mnd_seq`), sak (`ev05_sak`) og stønadstype (`ev05_valg`).
- `ev_data_10` har innholdet i evalueringen, én rad per posisjon: diagnoser,
  sats, hjemmelbokstav og virkningstidspunkt. Tabellen har person og måned, men
  ikke sak eller stønadstype.

Infotrygd har bekreftet at `ev05_sak` kobler `ev_valg_05` til saken. Hvordan en
rad i `ev_data_10` hører til én rad i `ev_valg_05`, er ikke avklart. Har
personen én evaluering i måneden, er koblingen entydig. Har personen flere
GH-saker med evaluering i samme måned, vet vi ikke hvilken sak radene i
`ev_data_10` gjelder.

Diagnosene og hjemmelbokstaven er helseopplysninger (GDPR art. 9). Kobler vi
feil, ser saksbehandleren diagnosen fra en annen sak, for eksempel
hjelpestønadssaken i en sak om grunnstønad.

## Beslutning

1. **Person og måned.** `ev_data_10` kobles til `ev_valg_05` på
   `ev01_personkey` og `ev05_aar_mnd_seq`. Det er de eneste kolonnene tabellene
   har felles som identifiserer evalueringen.
2. **Saken velger måneden.** Vedtaket kobles til GH-saken i `sa_sak_10`, og
   saken til `ev_valg_05` på `ev05_sak`. Siste måned for saken avgjør hvilke
   rader i `ev_data_10` vi henter.
3. **Vi henter bare fem posisjoner.** `055`, `100`, `150`, `160` og `165`
   (diagnose 1 og 2, sats, hjemmelbokstav og virkningstidspunkt). Andre
   posisjoner leses ikke.
4. **Hvert felt vises bare når verdien er sikker.** For hver posisjon samler vi
   alle radene i måneden, uansett `ev10_vedtaks_dag` og sak. Feltet fylles når
   alle radene har samme verdi og posisjonen har minst én rad per GH-sak i
   måneden. Ellers er feltet tomt, og posisjonen står i `motstridendeFelt`.
   Saksbehandleren må lese den i Infotrygd. De andre feltene vises som vanlig.
   Dette gjelder også når personen har flere saker i måneden.

Vi viser heller et tomt felt enn en verdi som kan tilhøre en annen sak. Vi
viser heller ikke en verdi bare fordi den er nyest. `ev10_vedtaks_dag` sier
ikke hvilken sak raden gjelder.

## Alternativer vurdert

### Alternativ A: Koble på `endret_i_kilde`

Begge tabellene har `endret_i_kilde`. Ideen var at radene for samme evaluering
blir skrevet omtrent samtidig, slik at nærmeste tidsstempel peker ut saken. Vi
målte dette i dev 2026-10-20.

I måneder med én sak, der koblingen er kjent, sammenlignet vi hver rad i
`ev_data_10` (1 286 459 rader) med nærmeste rad i `ev_valg_05`:

| Persentil | Avstand |
|-----------|---------|
| 50 | 52 min (3 106 s) |
| 90 | 69 min (4 136 s) |
| 99 | 74 min (4 444 s) |
| Maks | 75 min (4 508 s) |

Ingen rader lå nær hverandre i tid. Når saksbehandleren lagrer evalueringen,
skrives begge radene samtidig, så avstanden burde vært noen sekunder. En avstand
på rundt én time tyder på at `endret_i_kilde` er tidspunktet en lastejobb skrev
raden, og at tabellene lastes i ulike kjøringer.

I de 13 181 personmånedene med flere saker sammenlignet vi siste
`endret_i_kilde` per sak. Minste avstand og median var 0 sekunder. Minst
halvparten av månedene har altså nøyaktig samme tidsstempel på alle sakene, og
da kan ikke tidsstempelet skille dem.

Forkastet.

### Alternativ B: Andre kolonner i `ev_data_10`

Vi har sjekket `ev10_ps_type`, `ev10_des_kode`, `opprettet` og
`ev10_mottatt_dato` (mot `s10_mottattdato`). `ev10_ps_type` (P, S, T) og
`ev10_des_kode` (tom) er like for grunnstønad og hjelpestønad. Ingen av
kolonnene kobler raden til én sak.

Forkastet.

## Omfang

Målt i dev-replikaen 2026-10-20. En personmåned er én person og én
`ev05_aar_mnd_seq` med minst én rad i `ev_valg_05` der `ev05_kap = 'GH'` og
`ev05_tekst_kode = 'EVGH'`.

| Måling | Antall | Andel |
|--------|--------|-------|
| Personmåneder med evaluering | 175 341 | 100 % |
| Personmåneder med flere saker | 13 181 | 7,5 % |
| ... med samme stønadstype | 2 633 | 1,5 % |
| ... med grunnstønad og hjelpestønad | 10 548 | 6,0 % |
| Personer med evaluering | 145 293 | 100 % |
| Personer med flere saker i minst én måned | 13 066 | 9,0 % |

Fire av fem personmåneder med flere saker gjelder både grunnstønad og
hjelpestønad. Den femte har to saker med samme stønadstype, og der hjelper det
ikke å kjenne stønadstypen.

### Grunnlaget for beslutning 3 og 4

Målt i dev-replikaen 2026-10-20.

- **Alle fem posisjonene finnes i hver personmåned.** Det gjelder alle 162 160
  personmånedene med én sak. En verdi som mangler, ligger som tom tekst, for
  eksempel i 111 092 rader i posisjon `100`. Tom tekst mot en verdi blir
  derfor en konflikt. Det betyr ikke at feltet mangler.
- **Hver sak gir én rad per posisjon.** Med én sak har en posisjon én rad i
  98,2 % av tilfellene. Med flere saker har posisjonene like mange rader som
  saker i 13 177 til 13 180 av 13 181 personmåneder. Én personmåned har færre
  rader enn saker, og fem har flere.
- **`ev10_vedtaks_dag` trengs ikke.** Uten den får høyst 162 posisjonsmåneder
  med én sak ulik tekst og blir konflikter: `055` 8, `100` 35, `150` 25 og
  `160` 94.
- **Andre evalueringer blandes ikke inn.** Samme måned kan ha evalueringer med
  andre tekstkoder i GH: `EVBE` (86 481 personmåneder), `EVZ1` (8 823), `EVGS`
  (6 410), `EVOM` (2 132) og fem mindre. Disse skriver også til `055`, `100`,
  `150` og `160` i måneder uten `EVGH`, for eksempel `100` i 37 699
  personmåneder. I måneder med `EVGH` gir de ingen ekstra rader, se punktet
  over. `165` brukes bare av `EVGH`. De rundt 100 andre posisjonene i
  `ev_data_10` kommer i hovedsak fra de andre tekstkodene.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

Beslutningen hindrer at helseopplysninger fra én sak vises på en annen sak.
Vi henter færre opplysninger enn før, fordi spørringen bare leser fem
posisjoner. Hvem som får opplysningene, er uendret. `sak` auditlogger ikke
oppslaget ennå, se ADR-0009.

### Plattform

Ingen nye ressurser.

### Team-påvirkning

Frontend viser hvert felt i `motstridendeFelt` med beskjed om å lese det i
Infotrygd.

## Konsekvenser

### Positive

- En verdi vises ikke på feil sak, så lenge hver sak gir minst én rad per
  posisjon. Målingene viser at den gjør det.
- Saksbehandleren ser de feltene som er sikre, også når andre felt i samme
  evaluering er i konflikt.
- Koblingen avhenger ikke av når replikeringen skriver radene.
- Ugyldige data gir ikke lenger 500.

### Negative

- Saksbehandleren må lese felt i `motstridendeFelt` i Infotrygd.
- Vi får ikke eldre evalueringer for saken når et felt i siste måned er i
  konflikt.
- Vi viser ikke posisjoner med ukjent betydning, for eksempel `265`, `300` og
  `332`.

### Risiko

- Ingen kolonne sier hvilken sak eller tekstkode en rad kommer fra. Regelen
  bygger på antall rader. Har én sak to like rader i en posisjon og den andre
  saken ingen, blir antallet likt antall saker, og vi viser verdien. Det kan
  høyst gjelde de fem personmånedene med flere rader enn saker.
- En fremmed rad med samme verdi som `EVGH`-raden vises. Det kan høyst gjelde
  de 177 posisjonsmånedene med én sak og flere rader i våre posisjoner.
- Målingene gjelder dev. Kjør dem på nytt før en ny posisjon tas i bruk.
- Hvis `ev05_sak` ikke alltid stemmer med `sa_sak_10`, kan saken få feil måned.
  I dev traff `ev05_sak` en sak i 99,7 % av 20 000 rader i `ev_valg_05`.
- Har Infotrygd en kobling mellom radene som ikke replikeres, kan regelen være
  strengere enn nødvendig.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [ ] Spør Infotrygd om det finnes en kolonne eller tabell som kobler
      `ev_data_10` til én rad i `ev_valg_05`.
