# 04: Satsbeløp i kode

**What to build:** Systemet kan slå opp satsbeløpet for en sats (1–6) og en dato. Beløpene er månedsbeløpene fra [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/v1-06-03) fra 1.1.1997, i en Kotlin-liste i et `object`, som `SatsService` i familie-ba-sak. Se ADR-0014 under «Satsbeløpene ligger i kode».

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Hver rad har sats, månedsbeløp som `BigDecimal`, gyldig fra og med og gyldig til og med. Siste rad per sats er åpen.
- [ ] Listen dekker alle satsendringer i rundskrivet fra 1.1.1997, også 1.7.2023. År med uendrede satser gir ingen ny rad.
- [ ] Satsene under «Satser pr. 1.1.2024» gjelder fra 1.1.2024. En kommentar viser til aksjonspunktet i ADR-0011 om datoen.
- [ ] Oppslag for en dato før 1.1.1997 kaster en feil. Systemet bruker aldri nærmeste beløp.
- [ ] En test sjekker at periodene for hver sats ikke overlapper og ikke har hull.
- [ ] En test tar stikkprøver mot rundskrivet, blant annet sats 1 og 6 i 1997, 2023 og 2024.
- [ ] Koden er ren Kotlin uten Spring, etter ADR-0008.
