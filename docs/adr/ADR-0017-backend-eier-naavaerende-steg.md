# ADR-0017: Backend eier gjeldende steg i behandlingen

**Dato:** 2026-11-18
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad
**Bygger på:** [ADR-0004](ADR-0004-vilkaarssteg-utleder-perioder-med-rett.md),
[ADR-0014](ADR-0014-tilkjent-ytelse-fra-ekstrautgifter-og-trygdetid.md)

## Kontekst

ADR-0004 lot backend fullføre vilkårssteget, men utsatte et felles stegbegrep
til neste steg kom. Nå finnes to steg som fullføres i backend: inngangsvilkår
og sats og trygdetid. Frontend holdt styr på hvilke steg som var ferdige, og en
klient kunne hoppe rett til simulering, vedtak eller send til beslutter.

Det eneste vernet mot en foreldet beregning var at endring av en periode
slettet tilkjent ytelse. Ticket 22 vil beholde tilkjent ytelse og i stedet
kreve at saksbehandler fullfører stegene på nytt.

## Beslutning

### Behandlingen lagrer gjeldende steg

`behandling.gjeldende_steg` lagrer ett av `ÅRSAK`, `INNGANGSVILKÅR`,
`SATS_OG_TRYGDETID` og `VEDTAK_OG_BREV`. Alle steg før gjeldende steg er
fullført. Steget er atskilt fra `BehandlingStatus`, som beskriver hvor
behandlingen er i totrinnskontrollen.

Hvert steg har et eksplisitt felt `rekkefølge`, som brukes når steg sammenlignes.
Rekkefølgen verdiene står i, har ingen betydning.

### Fullføring setter steget direkte

`BehandlingStegService` håndterer overganger og tilbakeføring. Tjenestene som
fullfører steg eller endrer grunnlaget, beholder tilgangssjekker, beregninger,
endringshistorikk og transaksjoner.

| Handling | Nytt gjeldende steg |
|---|---|
| Lagre ny eller endret årsak | `INNGANGSVILKÅR` |
| Lagre samme årsak når steget står senere | Uendret, ingenting lagres |
| Fullføre inngangsvilkår med perioder | `SATS_OG_TRYGDETID` |
| Fullføre inngangsvilkår uten perioder | `VEDTAK_OG_BREV` |
| Fullføre sats og trygdetid | `VEDTAK_OG_BREV` |
| Endre eller slette medlemskap eller diagnose | `INNGANGSVILKÅR` hvis steget står senere |
| Endre eller slette ekstrautgifter eller trygdetid | `SATS_OG_TRYGDETID` hvis steget står senere |
| Underkjenning | Uendret |

En endring flytter aldri steget fremover. «Neste» på et tidligere steg uten
endringer setter steget tilbake. Saksbehandler må da fullføre de senere stegene
på nytt, og sats og trygdetid regner ut tilkjent ytelse på nytt.

Fullføring av inngangsvilkår svarer med `nesteSteg` som et `BehandlingSteg`.
Det erstatter enumen `NesteStegEtterInngangsvilkår` fra ADR-0014, der
`SIMULERING` nå er `VEDTAK_OG_BREV`.

### Backend avviser steg som ikke er nådd

`BehandlingStegService.validerKanEndreSteg` krever at behandlingen er redigerbar
og at steget er nådd. Sjekken brukes for å fullføre inngangsvilkår eller sats og
trygdetid, lagre årsak, vedtak eller brevmottakere, mellomlagre brev og sende til
beslutter. Når perioder endres, sjekker backend behandlingen som allerede er hentet.

Simulering bare leser. Den krever `VEDTAK_OG_BREV` mens behandlingen er
redigerbar, men ikke etter sending til beslutter.

Godkjenning har ikke eget stegvern. Status `FATTER_VEDTAK` nås bare gjennom
send til beslutter, som krever `VEDTAK_OG_BREV`.

Frontend låser senere faner mens behandlingen er redigerbar, og sender en
direkte URL til gjeldende steg.

### Eksisterende behandlinger starter på årsak

Migreringen setter alle behandlinger til `ÅRSAK`. Det finnes ingen åpne
behandlinger i prod. Fanene låses bare mens behandlingen er redigerbar, så
avsluttede behandlinger kan fortsatt leses.

## Alternativer vurdert

### Egen tabell med tilstand per steg

Gir status og historikk per steg, som i ba-sak. Endringshistorikken har allerede
fullføringene og periodeendringene, og regelen om aldri å flytte fremover måtte
regnes ut fra flere rader. Forkastet.

### Merke tilkjent ytelse som utdatert

Et flagg eller en hash av grunnlaget på tilkjent ytelse oppdager en foreldet
beregning uansett vei, men løser ikke navigasjonen. Kan legges til senere.

## Konsekvenser

- Tilkjent ytelse slettes fortsatt ved periodeendring. Slettingen fjernes i en
  egen PR når vernet er i bruk, og erstatter da ticket 11.
- Et lagret vedtak og mellomlagret brev beholdes ved tilbakeføring. Saksbehandler
  kan sende et vedtak som ble lagret før grunnlaget endret seg.
- Faktagrunnlag fra PDL har ikke stegvern. Det endrer opplysningene saksbehandler
  vurderer, ikke grunnlaget beregningen leser.
- Backend låser ikke behandlingen. To samtidige kall på samme behandling, for
  eksempel lagring av trygdetid og «Neste» før lagringen har svart, kan gi en
  fullført beregning som ikke tar med siste endring. Bare ansvarlig saksbehandler
  kan skrive, så vi godtar risikoen inntil videre.

## Aksjonspunkter

- [ ] Fjern slettingen av tilkjent ytelse i `PeriodeMedBegrunnelseService` (PR B).
- [ ] Vurder om send til beslutter skal kreve at vedtaket er lagret etter siste
      fullføring av sats og trygdetid.
