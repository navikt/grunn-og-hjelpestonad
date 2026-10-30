# Avhengigheter mellom ticketene

Ticketene ligger i [issues/](issues/), og spec-en i [spec.md](spec.md). Ticket 01,
02 og 04–12 er slettet (ADR-0013). Ticket 13 reverserer commit `1a4d1b3` fra
ticket 01.

- Hvite tickets er `ready-for-agent`.
- Oransje tickets er `ready-for-human`. De er rød sone eller krever kunnskap om
  Infotrygd.
- Gule tickets er `needs-triage`.
- Grønne tickets er løst.
- Grå sekskanter er blokkeringer utenfor repoet, tegnet med stiplede piler.

24 har ingen piler fordi ingen andre tickets avhenger av den.

```mermaid
flowchart TD
    T03["03 Avklar perioder og<br/>utbetalt beløp fra Infotrygd"]
    T13["13 Startmåned tilbake<br/>på tilkjent ytelse"]
    T14["14 Validering av Infotrygd<br/>og forhåndsvisning"]
    T15["15 Kontroll 1 og<br/>simulering"]
    T16["16 Opprett og iverksett<br/>migrering"]
    T17["17 Lås mot<br/>dobbel migrering"]
    T18["18 vedtaksmelding og<br/>sjekk av opphør"]
    T19["19 Sperre mot<br/>førstegangsbehandling"]
    T20["20 «Migrer» i frontend"]
    T21["21 Revurdering<br/>kopierer vilkår"]
    T22["22 Revurdering flytter<br/>startmåned bakover"]
    T23["23 Test av hele<br/>migreringen"]
    T24["24 Unik indeks mot<br/>to åpne behandlinger"]
    T25["25 Opphav og maskinelt<br/>merke på vilkår"]
    T26["26 Gjeldende perioder<br/>kuttes ved opphør"]
    T27["27 Sammenslåtte perioder<br/>med Infotrygd-beløp"]

    OKO{{"Økonomi: motregner OS?"}}
    OPP{{"Oppdrag-delen<br/>(ADR-0011)"}}

    T03 --> T14
    T03 --> T15
    T26 --> T27
    T27 --> T14
    T27 --> T15

    T13 --> T16
    T14 --> T16
    T15 --> T16
    T17 --> T16
    T25 --> T16
    T21 --> T25
    OPP -.-> T16

    T16 --> T18
    T14 --> T19
    T14 --> T20
    T16 --> T20

    T13 --> T22
    T21 --> T22
    OPP -.-> T22
    OKO -.-> T22

    T16 --> T23
    T17 --> T23
    T18 --> T23

    classDef menneske fill:#ffe5c2,stroke:#c77300
    classDef triage fill:#fff6c2,stroke:#a68b00
    classDef ekstern fill:#f1f1f1,stroke:#707070,stroke-dasharray: 4 3
    classDef loest fill:#d9f2d9,stroke:#2e7d32

    class T13,T14,T15,T17,T22,T26,T27 menneske
    class T03 loest
    class T24 triage
    class OKO,OPP ekstern
```

03 er løst. 21 kan startes med en gang av en agent. 13, 17 og 26 kan også
startes, men er rød sone og skrives av en utvikler.
