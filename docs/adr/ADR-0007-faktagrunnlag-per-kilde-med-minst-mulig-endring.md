# ADR-0007: Faktagrunnlag per kilde, med minst mulig endring

**Dato:** 2026-10-01
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

Vilkårene vurderes ut fra faktagrunnlag som hentes fra registre, for
eksempel PDL.

Faktagrunnlaget må kunne etterprøves: Saksbehandler og beslutter skal se
hva som ble hentet, når det ble hentet, og om kilden svarte uten treff.

## Beslutning

1. **Ett faktagrunnlag per kilde.** Hver kilde lagres for seg. Vi lager
   ikke et samlet grunnlag per vilkår. Samme kilde kan brukes av flere vilkår.
2. **Minst mulig endring før lagring.** Opplysningene lagres slik kilden
   leverte dem. Vi oversetter ikke koder, slår ikke sammen perioder og
   filtrerer ikke på gyldighet. Tolkning, som å utlede medlemskapsperioder,
   skjer når vilkåret vurderes.
3. **Kolonner, ikke JSONB.** Hver opplysningstype får sin egen tabell, med én
   kolonne per felt og feltnavnene fra kilden.
4. **Navn etter kilden.** Pakker og klasser navngis etter kilden
   (`faktagrunnlag/medl`, `faktagrunnlag/pdl`), ikke etter
   vilkåret. Alle tabeller for faktagrunnlag har prefikset
   `faktagrunnlag_`, etterfulgt av kilden og opplysningstypen, for
   eksempel `faktagrunnlag_medl_unntaksperiode` og
   `faktagrunnlag_pdl_statsborgerskap`.
5. **Hentingen lagres for seg.** Én felles tabell,
   `faktagrunnlag_henting`, har én rad per behandling og
   kilde, med tidspunktet for hentingen. Da kan vi skille «hentet, ingen
   treff» fra «ikke hentet», og radene med faktagrunnlag trenger ikke
   eget hentetidspunkt.
6. **Automatisk henting, og ny henting ved behov.** Faktagrunnlaget
   hentes i samme transaksjon som behandlingen opprettes. Svarer ikke kilden,
   feiler opprettelsen, og saksbehandler må prøve på nytt. Da har alle nye
   behandlinger grunnlag fra kildene, og resten av løsningen trenger ikke
   håndtere behandlinger uten grunnlag.
   Saksbehandler kan hente på nytt så lenge behandlingen kan redigeres. En ny
   henting erstatter det som er lagret fra samme kilde for denne behandlingen.
   Faktagrunnlaget i andre behandlinger endres aldri.

## Alternativer vurdert

### Alternativ A: Ett samlet grunnlag per vilkår, som K9

- **Fordeler:** Alt vilkåret trenger, ligger samlet. Vurderingen blir enkel å
  skrive.
- **Ulemper:** Kildene blandes, og samme register må hentes og lagres på nytt
  for hvert vilkår som trenger det. Endres vilkåret, må grunnlaget endres også.

### Alternativ B: Lagre hele svaret som JSONB

- **Fordeler:** Svaret lagres helt uendret, og nye felt fra kilden krever ingen
  migrering.
- **Ulemper:** Databasen kjenner ikke strukturen. Vi kan ikke ha unik-krav og
  sjekker på feltene, og spørringer og migreringer blir vanskeligere.

### Alternativ C: Én tabell per opplysningstype i hver kilde, med kolonner (valgt)

- **Fordeler:** Vi ser hva kilden sa, databasen håndhever strukturen, og samme
  kilde kan brukes av flere vilkår.
- **Ulemper:** Flere tabeller, og nye felt fra kilden krever migrering.

### Alternativ D: Opprett behandlingen selv om kilden ikke svarer

- **Fordeler:** Saksbehandler kan opprette behandlinger selv om en kilde er
  nede.
- **Ulemper:** Alle deler av løsningen må håndtere behandlinger uten grunnlag.
  Feiler lagringen av grunnlaget, ruller transaksjonen uansett tilbake
  opprettelsen. Da beskytter dette bare mot feil fra kilden.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

Faktagrunnlaget inneholder personopplysninger. Vi lagrer bare
opplysningstypene vilkårene trenger, ikke alt kilden kan levere. Tilgangen
sjekkes mot tilgangsmaskinen før grunnlaget hentes eller vises.

## Konsekvenser

### Positive

- Det er enkelt å se hva kilden sa, og når den ble spurt.
- Tolkningsregler kan endres uten å hente eller migrere grunnlaget på nytt.

### Negative

- Vurderingen må selv tolke koder og sette sammen opplysninger fra flere
  kilder.
- Hver ny opplysningstype krever en ny tabell eller migrering.

### Risiko

Er en kilde nede, kan ingen behandlinger opprettes før den svarer igjen.

Automatisk henting når behandlingen opprettes forutsetter at vi har et token å
hente med. I dag opprettes behandlinger bare av saksbehandler. Opprettes de
senere av et system, trenger vi maskin-til-maskin-tilgang til kildene, ellers
feiler opprettelsen.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [ ] Lag faktagrunnlaget fra MEDL.
- [x] Lag faktagrunnlaget fra PDL.
