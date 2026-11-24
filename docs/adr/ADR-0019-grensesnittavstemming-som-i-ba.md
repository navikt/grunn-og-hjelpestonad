# ADR-0019: Grensesnittavstemming følger BA-mønsteret

**Dato:** 2026-11-24
**Status:** Godkjent

## Kontekst og beslutning

Vi bruker `familie-oppdrag-backend` og BA-mønsteret for grensesnittavstemming mot Oppdrag: et lagret tidsvindu med stabil
`avstemmingId`, `maxAntallFeil = 1` og opprettelse av neste task etter vellykket svar.
Vi velger dette fordi mønsteret har vært brukt i produksjon i flere år og har fungert for BA.
Vi viderefører den etablerte håndteringen av usikkert leveringsutfall fremfor å innføre en egen
transaksjons- eller gjenopptakelsesløsning i mottakeren nå.

Ved en exception stopper tasken med status `STOPPET`, slik at utfallet kan undersøkes før
manuell omkjøring. Omkjøring bruker eksisterende task med samme payload og `avstemmingId`.
Neste vindu opprettes i samme databasetransaksjon som fullføringen av gjeldende task.
Kjeden startes eksplisitt med `GrensesnittavstemmingService.opprettFørsteTask`.
Operatørinngang avtales før aktivering, og en ny kjede skal ikke startes hvis en allerede er aktiv.

## Aksepterte begrensninger

Mottakeren sender avstemmingsmeldingene enkeltvis til MQ og registrerer forespørselens
`avstemmingId` som fullført etter sending. ID-en hindrer ny sending av en registrert fullført
avstemming, men gjenopptar ikke en delvis sending. Mapperen genererer en egen avstemmings-ID
til MQ-meldingene ved hver kjøring. Feil under sending kan derfor kreve undersøkelse hos
mottakeren og avklaring med ansvarlige for Oppdrag/avstemming før manuell omkjøring.

Taskmotorens `Reaper` kan gjenopprette tasker etter krasj uavhengig av `maxAntallFeil`.
Hos oss settes tasker som har stått i `KJØRER` i over fem minutter tilbake til `KLAR`, uten at
feiltelleren økes. Dette kan gi ny sending dersom mottakeren ennå ikke har registrert fullføring.
Vi beholder standardoppførselen i taskmotoren.

Vi aksepterer disse begrensningene ut fra produksjonserfaringen med BA-mønsteret.

## Alternativ vurdert

En felles MQ-transaksjon kan hindre delvis sending, men løser ikke alene krasjvinduet mellom
MQ-commit og registrering av fullføring i Postgres. Vi innfører ikke dette eller lagret fremdrift
per melding nå, fordi det krever en annen leveringsmekanisme i mottakeren enn den BA bruker.

## Referanser

- [BA-tasken](https://github.com/navikt/familie-ba-sak/blob/main/src/main/kotlin/no/nav/familie/ba/sak/task/GrensesnittavstemMotOppdrag.kt)
- [Mottakerens avstemmingsservice](https://github.com/navikt/familie-oppdrag-backend/blob/91e21eec09491011e0aa9f90a7d0ffc7c660d55f/src/main/kotlin/no/nav/familie/oppdrag/service/GrensesnittavstemmingService.kt)
- [Mottakerens MQ-konfigurasjon](https://github.com/navikt/familie-oppdrag-backend/blob/91e21eec09491011e0aa9f90a7d0ffc7c660d55f/src/main/kotlin/no/nav/familie/oppdrag/config/OppdragMQConfig.kt)
