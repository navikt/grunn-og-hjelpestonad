# 17: Avklar institusjon sammen med ekstrautgiftene

**What to build:** En beslutning om hvordan vilkåret institusjon etter § 6-8 kommer tilbake i løsningen, designet sammen med nødvendige ekstrautgifter. Løsningen kan ikke tas i bruk i prod før dette er på plass, se ADR-0006. Fag må svare.

**Blocked by:** 15

**Status:** needs-info

Spec: [spec.md](../spec.md). Bakgrunn: ADR-0003 og ADR-0006 under «Institusjon tas ut av løsningen inntil videre».

- [ ] Spørsmålene under har svar under `## Answer`.
- [ ] Det finnes en ADR for institusjonsvilkåret, og tickets for å bygge det.

## Det vi vet

Opphold på institusjon gir ikke rett i perioden. Unntaket er når søker har ekstrautgifter ut over det institusjonen dekker. Da fastsettes satsen bare ut fra disse utgiftene. Verdiene i `Oppholdstype` og `Unntakshjemmel` fra ADR-0003 ble aldri sjekket med fag.

## Spørsmål

1. Er «utgifter institusjonen ikke dekker» et flagg på utgiftsposten, eller et eget vilkår med ja eller nei per periode?
2. Hvilke oppholdstyper og unntak finnes, og hvor får vi opplysningene om opphold fra?
3. Hvilken måned faller retten bort, og hvilken måned kommer den tilbake etter et opphold?
4. Hvordan ser institusjon ut i Infotrygd, og hva betyr det for migrering?

## Comments
