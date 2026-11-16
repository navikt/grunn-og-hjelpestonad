# 19: Lengdegrense og `toString` for begrunnelse

**What to build:** Begrunnelsen på alle perioder med begrunnelse har en lengdegrense i backend og holdes utenfor `toString`. Frontend begrenser feltet til 2000 tegn, men backend sjekker det ikke. Begrunnelsen er fritekst og kan inneholde helseopplysninger, men `toString` på data-klassene tar den med, så den kan havne i logger og feilmeldinger.

**Blocked by:** None (can start immediately)

**Status:** needs-triage

Gjelder medlemskap, diagnose, nødvendige ekstrautgifter og trygdetid.

- [ ] Backend avviser en begrunnelse over 2000 tegn med `UgyldigInput`, i den felles servicen for perioder med begrunnelse.
- [ ] Databasen har en CHECK-constraint på lengden, eller ADR-en forklarer hvorfor ikke.
- [ ] `toString` på entiteter og requester skjuler begrunnelsen.
- [ ] `toString` på `VilkårDiagnose`, `VilkårEkstrautgifter`, `Ekstrautgift`, `EkstrautgiftDiagnose` og requestene skjuler diagnosekodene. Koblingen mellom person og diagnose er en helseopplysning (GDPR artikkel 9).
- [ ] En test sjekker grensen og at `toString` ikke inneholder begrunnelsen.

## Comments
