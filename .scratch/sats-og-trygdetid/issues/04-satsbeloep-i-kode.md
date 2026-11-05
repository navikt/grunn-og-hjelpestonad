# 04: Satsbeløp i kode

**What to build:** Systemet kan slå opp satsbeløpet for en sats (1–6) og en dato. Beløpene er månedsbeløpene fra [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/v1-06-03) fra 1.1.1997, i en Kotlin-liste i et `object`, som `SatsService` i familie-ba-sak. Se ADR-0014 under «Satsbeløpene ligger i kode».

**Blocked by:** None (can start immediately)

**Status:** resolved

Spec: [spec.md](../spec.md)

- [x] Hver rad har sats, månedsbeløp i hele kroner som `Int`, gyldig fra og med og gyldig til og med. Siste rad per sats er åpen.
- [x] Listen dekker alle satsendringer i rundskrivet fra 1.1.1997, også 1.7.2023. År med uendrede satser gir ingen ny rad.
- [x] Satsene under «Satser pr. 1.1.2024» gjelder fra 1.1.2024. En kommentar viser til aksjonspunktet i ADR-0011 om datoen.
- [x] Oppslag for en dato før 1.1.1997 kaster en feil. Systemet bruker aldri nærmeste beløp.
- [x] En test sjekker at periodene for hver sats ikke overlapper og ikke har hull.
- [x] En test tar stikkprøver mot rundskrivet, blant annet sats 1 og 6 i 1997, 2023 og 2024.
- [x] Koden er ren Kotlin uten Spring, etter ADR-0008.

## Comments

Løst i `Satsbeløp.kt` og `SatsbeløpTest.kt` under `apps/sak/.../tilkjentytelse/`. Rundskrivet har ingen satser for 2001, og for sats 2 i 2012 stemmer ikke månedsbeløpet med årsbeløpet. Teamet har bestemt at satsene fra 2000 gjelder ut 2001, og bruker 964 kroner for sats 2 i 2012, som Infotrygd utbetalte, ikke 968 fra rundskrivet. Begge er ført opp som avklarte aksjonspunkter i ADR-0011.
