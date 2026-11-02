# 27: Sammenslåtte gjeldende perioder med Infotrygd-beløp

**What to build:** Saksbehandler ser gjeldende perioder fra Infotrygd slått sammen og med Infotrygd-beløp, akkurat slik migreringen vil bruke dem. Da kan saksbehandler se i oppslaget om migreringen blir riktig.

**Blocked by:** 26. Begge endrer reglene for gjeldende perioder.

**Status:** ready-for-human

🔴 Rød sone: sammenslåingen og regelen for Infotrygd-beløp. Utvikleren skriver den rene funksjonen og testene. Kolonnen i frontend er grønn sone.

Spec: [spec.md](../spec.md). Beslutningene står i [ticket 03](03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md) og ADR-0012.

- [ ] Hver gjeldende periode har Infotrygd-beløpet fra grunnstønad-delytelsen på vedtaket.
- [ ] Vedtaket avgjør perioden. Delytelsen endrer ikke fom eller tom.
- [ ] Dekker delytelsen mindre enn vedtaksperioden etter kuttet ved opphør, i hvilken som helst periode, gir oppslaget `UgyldigInput`.
- [ ] Har delytelsen en annen periode enn vedtaket i den siste gjeldende perioden, gir oppslaget samme feil, også når delytelsen dekker mer. I tidligere perioder er det ikke en feil når delytelsen dekker mer.
- [ ] Har vedtaket ingen delytelse, gir oppslaget samme feil.
- [ ] Feilen har teksten «Vedtak {vedtakId} gjelder {fom}–{tom}, men delytelsen med beløpet i Infotrygd gjelder {delytelseFom}–{delytelseTom}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.» Er vedtaket eller delytelsen løpende, står det «fra {fom}».
- [ ] Sats og trygdetid kommer fra raden i `t_gh` med nyest registreringstidspunkt.
- [ ] Perioder som følger etter hverandre, slås sammen når Infotrygd-beløp, sats, trygdetid, diagnose og hjemmelbokstav er like. En tom verdi regnes som en egen verdi.
- [ ] «Gjeldende perioder» i frontend har en kolonne for beløp.
- [ ] Rene tester dekker: like perioder som slås sammen, perioder som skilles på hvert av de fem feltene, delytelse som slutter før vedtaket, delytelse som starter etter vedtaket, delytelse som varer lenger enn vedtaket i en tidligere periode og i den siste perioden, vedtak uten delytelse og arvet `t_gh`.
- [ ] Teksten i feilmeldingen er gjennomgått av `@forfatter`.
