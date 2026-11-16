# 01: Iverksetting med kjeding mot OS

**What to build:** Når beslutter godkjenner et vedtak som innvilger eller opphører, lager `apps/sak` oppdraget, kjeder det mot det som er sendt på fagsaken før, lagrer det og sender det til OS gjennom `familie-oppdrag-backend`. En egen task poller kvitteringen til OS svarer `KVITTERT_OK`.

**Blocked by:** None

**Status:** ready-for-human

🔴 Rød sone: kjedingen (`lagUtbetalingsoppdrag`, spørringene i `OppdragResultatRepository`) og sperren mot nytt oppdrag før forrige er kvittert. Feil her gir feilutbetaling.

ADR: [ADR-0011](../../../docs/adr/ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md), [ADR-0013](../../../docs/adr/ADR-0013-migrering-fra-infotrygd-som-i-ef.md) (opphør før første andel)

- [x] `oppdrag_resultat` og `utbetalingsandel` (V52). En kjede er én ytelsestype, ikke én klassekode. Oppdragene på en fagsak sorteres på `opprettet_tid`, som i EF og BA.
- [x] Kjeding med forrige andeler og siste andel per kjede. Tester for førstegangsbehandling, nytt beløp, avkorting, opphør av alle andeler, ingen endring og ny andel etter fullt opphør.
- [x] Oppdraget lages og lagres i en egen transaksjon og committes før det sendes. Et nytt forsøk sender det lagrede oppdraget. Fagsaken låses ikke: vi stoler på at fagsaken bare kan ha én åpen behandling, og på sperren under.
- [x] 409 fra `familie-oppdrag-backend` regnes som sendt.
- [x] Nytt oppdrag stopper med 409 hvis forrige oppdrag på fagsaken ikke har status `KVITTERT_OK` eller `INGEN_ENDRING`.
- [x] Status hentes med identen oppdraget ble sendt med, så et identbytte gir ikke 404.
- [x] Iverksetting og status bruker apptoken. Simulering bruker fortsatt OBO.
- [x] `IverksettOppdragTask` ved godkjenning og `HentOppdragStatusTask`.
- [x] Testendepunktet bruker samme kjeding og lagring.
- [ ] Bekreft i dev at `familie-oppdrag-backend` godtar apptoken: én førstegangsbehandling og én revurdering, begge med `KVITTERT_OK`.

## Kjente begrensninger

- Et avvist oppdrag stopper alle nye oppdrag på fagsaken. Det finnes ingen måte å rette det på ennå, verken i koden eller i et skjermbilde.
- Godkjenning setter behandlingen til `FERDIGSTILT` med én gang, før oppdraget er kvittert. En revurdering kan derfor godkjennes mens forrige oppdrag fortsatt ligger på kø. Da får `IverksettOppdragTask` 409 fra sperren, feiler tre ganger og stopper. Noen må starte den på nytt når forrige oppdrag er kvittert. Det løses når behandlingen først blir `FERDIGSTILT` ved `KVITTERT_OK`, som i EF.
- Av samme grunn beskytter «én åpen behandling per fagsak» ikke kjedingen før oppdraget er lagret. Feiler `IverksettOppdragTask` før lagringen (for eksempel mot PDL eller tilkjent ytelse), kan en revurdering godkjennes og kjedes først. Når den eldste tasken kjører igjen, kjedes den etter revurderingen og sender den gamle behandlingens andeler. Uten lås kan også to tasker på samme fagsak lese samme forrige andeler hvis de kjører samtidig. Primærnøkkelen på `behandling_id` beskytter bare mot to oppdrag for samme behandling.
- Testendepunktet oppretter ingen statustask. Etter et kall må noen hente status med GET-endepunktet, ellers stopper neste oppdrag på fagsaken.
- Avvist oppdrag eller manglende kvittering etter 100 forsøk gir en stoppet task. Saksbehandler ser ingenting, og ingen får varsel.
- Statustasken poller hvert 15. minutt på hverdager mellom 06 og 21, som i familie-ba-sak, men tar ikke hensyn til helligdager. Én helligdag bruker 60 av de 100 forsøkene, så to helligdager på rad (jul, påske) stopper tasken før OS åpner.
- Fagsaker som ble sendt fra testendepunktet før V52, har kjeder i OS som vi ikke kjenner. Ikke bruk dem.
- Testandelen (1000 kr for inneværende måned) brukes når tilkjent ytelse mangler. Den fjernes når issue 10 i sats-og-trygdetid er ferdig.
- `opphørAlleKjederFra` er alltid `null`. ADR-0013 beskriver når den skal settes: ved revurdering der startmåned flyttes bakover og er før første nye andel, eller det ikke finnes nye andeler. Det er ikke koblet inn ennå.

## Til neste PR

Funn fra review som ikke er rettet:

- Testendepunktet kan låse et utdatert oppdrag: finnes oppdraget fra før, sendes det lagrede, selv om tilkjent ytelse er endret etterpå.
- Et innvilget vedtak med tom tilkjent ytelse opphører hele kjeden. `beregn` skiller ikke mellom opphør og innvilgelse når tilkjent ytelse finnes uten andeler.
- Simulering uten endring mot forrige behandling gir 400 («Behandlingen endrer ikke utbetalingen»).
- `hentStatus` skriver kvitteringsmeldingen til secureLogs ved hver polling.

## Før prod

- Godkjenning sender oppdrag til OS i alle miljøer. Det finnes ingen bryter, så prod-kodene må være på plass før første godkjenning i prod.
- Testendepunktet må fjernes eller kreve godkjent vedtak. I dag kan en saksbehandler iverksette uten beslutter.
- Økonomi må gi fagområde og klassekode for prod. Dev bruker `IT28` og `GHGSORMG`.
- Teamet må bestemme hvor lenge `oppdrag_resultat` (med fnr i `utbetalingsoppdrag`) skal lagres, og føre lagringen inn i behandlingsoversikten.
- Varsling når en oppdragstask stopper.
