# Vurder å dele opp VedtakService

Status: needs-triage

## Bakgrunn

Oppfølging av [kommentaren i PR #160](https://github.com/navikt/grunn-og-hjelpestonad/pull/160#discussion_r4613058425): «Synes denne servicen egentlig gjør for mye, men tenker vi kan la det være for nå».

`VedtakService` håndterer henting, lagring og sletting av vedtak, validering av vedtaksresultat mot tilkjent ytelse og beregning og validering av beløpsperioder. Oppdelingen tas separat, ikke i PR-en som legger til vedtaksresultatvalideringen.

## Oppgave

Vurder hvilke ansvar som bør ligge i `VedtakService`, og hvilke som bør flyttes til andre tjenester eller rene funksjoner. Beskriv ansvarsgrensene før refaktoreringen. Følg ADR-0008 for deterministisk logikk utenfor Spring, og unngå nye tjenester som bare videresender kall.

## Akseptkriterier

- [ ] Kartlegg ansvar og kallere, og avklar en oppdeling før implementering.
- [ ] Behold valideringsregler, feilmeldinger, tilgangskontroller og transaksjonsgrenser.
- [ ] Oppdater testene slik at beregning, validering og koordinering testes på riktig nivå. Behold eksisterende dekning.
- [ ] Refaktoreringen endrer ikke API-kontrakter eller lagrede data og tas i en egen PR.

## Berørt kode

- `apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/vedtak/VedtakService.kt`
- `apps/sak/src/test/kotlin/no/nav/grunn/og/hjelpestonad/vedtak/VedtakServiceTest.kt`
- `docs/adr/ADR-0008-deterministisk-logikk-utenfor-spring.md`

## Comments

Oppdelingen er ikke bestemt. Ticketen dokumenterer oppfølgingen fra kodegjennomgangen.
