# ADR-0019: Vedtakslagring avgrenses til førstegangsbehandling

**Dato:** 2026-11-24
**Status:** Godkjent

## Beslutning

Vi innfører vedtakslagring og iverksetting for førstegangsbehandling med
innvilgelse eller avslag. Opphør, revurdering og tilpasning av historikk-API-et
tas i egne endringer for å holde denne PR-en avgrenset og enklere å gjennomgå.

Backend utleder vedtaksresultatet fra gjeldende behandlingsgrunnlag før sending
til beslutter. Ved ny sending etter underkjenning oppdateres eksisterende
innvilgelse eller avslag. En fullført førstegangsberegning uten andeler lagrer
ingen tilkjent ytelse. At behandlingen har nådd `VEDTAK_OG_BREV`, skiller dette
fra en beregning som ikke er fullført. Endringer i grunnlaget flytter
behandlingen tilbake til steget som må fullføres på nytt, se
[ADR-0017](ADR-0017-backend-eier-naavaerende-steg.md).

## Konsekvenser

Historikk-API-et leser fortsatt `grunnstønadPerioder` fra den gamle
barnetilsynsmodellen. Nye vedtak lagres uten disse periodene. API-et viser derfor
ikke stønadsperiodene for nye innvilgelser, og `fraErFørTidligsteVedtak` kan bli
`true` selv om forespurt dato er innenfor innvilgelsen. Dagens frontend bruker
ikke dette API-et. Tilpasningen til tilkjent ytelse følges opp separat.

Vi godtar denne begrensningen fremfor å endre historikkmodellen og innføre
opphør og revurdering i samme PR. Avgrensningen betyr ikke at disse flytene er
ferdig implementert eller verifisert.
