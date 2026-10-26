# Avhengigheter mellom ticketene

Ticketene ligger i [issues/](issues/), og spec-en i [spec.md](spec.md).

- Grønne tickets kan startes nå.
- Oransje tickets er `ready-for-human`.
- Grå sekskanter er blokkeringer utenfor repoet, tegnet med stiplede piler.

01 har ingen piler fordi ingen andre tickets avhenger av den.

```mermaid
flowchart TD
    T01["01 Fjern startmåned"]
    T02["02 Opprett migreringsbehandling<br/>med migreringsdato"]
    T03["03 Avklar perioder og<br/>utbetalt beløp fra Infotrygd"]
    T04["04 Avklar vilkår<br/>fra Infotrygd"]
    T05["05 Perioder fra Infotrygd<br/>og kontroll 1"]
    T06["06 Vilkår fra Infotrygd<br/>med kilde og utfylling"]
    T07["07 Kontroll 3:<br/>simulering 0 kr"]
    T08["08 Uten vedtaksbrev,<br/>med vedtaksmelding"]
    T09["09 Revurderingssperre"]
    T10["10 Migreringsdato<br/>tilbake i tid"]
    T11["11 «Endre migreringsdato»-<br/>behandling"]
    T12["12 Test av hele<br/>migreringsflyten"]

    OKO{{"Økonomi: motregner OS?"}}
    OPP{{"Oppdrag-delen<br/>(ADR-0011)"}}

    T02 --> T05
    T03 --> T05
    T02 --> T06
    T04 --> T06
    T02 --> T07
    T02 --> T08
    T02 --> T09

    T05 --> T10
    T07 --> T10
    OKO -.-> T10

    T05 --> T11
    T06 --> T11
    T07 --> T11
    T08 --> T11
    OKO -.-> T11

    T05 --> T12
    T06 --> T12
    T07 --> T12
    T08 --> T12
    OPP -.-> T12

    classDef klar fill:#ccf1d6,stroke:#06893a
    classDef menneske fill:#ffe5c2,stroke:#c77300
    classDef ekstern fill:#f1f1f1,stroke:#707070,stroke-dasharray: 4 3

    class T01,T02 klar
    class T03,T04 menneske
    class OKO,OPP ekstern
```
