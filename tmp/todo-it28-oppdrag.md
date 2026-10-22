# Todo: IT28 i grunn-og-hjelp-oppdrag

Repo: `navikt/grunn-og-hjelp-oppdrag` (fork av familie-oppdrag).

POC-en sender `Utbetalingsoppdrag` fra grunn-og-hjelpestonad med `fagSystem = "IT28"` (fagområde) og `klassifisering = "GHGSORMG"`, `satsType = MND`. Senere bytter vi til `GH` når økonomi er klar. Forken må derfor godta begge kodene.

## Må gjøres før første oppdrag kan sendes

- [ ] **Komponentkode for IT28.** `AvstemmingMapper.fagområdeTilAvleverendeKomponentKode` godtar bare `"GH"` og kaster `Error("$fagområde støttes ikke ennå")` for alt annet. `OppdragMapper.tilOppdrag110` kaller den for `Avstemming115.kodeKomponent`, så hvert IT28-oppdrag feiler før det legges på MQ.
  - Legg til `"IT28" -> "<komponentkode>"`. Koden må avklares med økonomi.
  - Behold `"GH" -> "GH"`.
  - Fjern `// TODO Hva er koder og fagområder for oss?` når kodene er avklart.
- [ ] **Test:** `OppdragMapper` med `fagSystem = "IT28"` gir `kodeFagomraade = IT28`, riktig `kodeKomponent` og `kodeKlassifik = GHGSORMG`.
- [ ] **Simulering:** Sjekk at simuleringsmapperen (via `familie-ws-proxy`) ikke har en egen hviteliste over fagområder. Ikke lest ennå.
- [ ] **Simulering, FagOmrådeKode:** `SimulertPostering.fagOmrådeKode` i familie-kontrakter er enumen `FagOmrådeKode`, og den har verken `IT28` eller `GH`. `FagOmrådeKode.fraKode("IT28")` kaster, så simulering feiler i forken før svaret sendes (ikke verifisert i forkens kode). Legg til koden i familie-kontrakter (krever ny versjon i begge repoene), eller la forken mappe ukjente koder. Grunn-og-hjelpestonad leser feltet som tekst, men verdien blir enum-navnet, ikke koden.
- [ ] **Kvittering:** Avklar med økonomi at OS sender kvitteringer for IT28 til `QA.Q1_GH_OPPDRAG.OPPDRAG_XML_KVITT` (preprod) og `QA.P_GH_OPPDRAG.OPPDRAG_XML_KVITT` (prod).
- [ ] **Enhet:** `OppdragMapper` har hardkodet enhet `8020` / `BOS`. Avklar med økonomi om det gjelder for grunnstønad under IT28.

## Må gjøres før avstemming (leveranse 2)

- [ ] Utvid `enum class Fagsystem { GH }` i `service/GrensesnittavstemmingService.kt` med `IT28`. Enumen brukes i `/api/{fagsystem}/fagsaker/siste-utbetalingsoppdrag` og i avstemmingen.
- [ ] Avklar at avstemmingskøen `QA.Q1_234.OB29_AVSTEMMING_XML` / `QA.P234.OB29_AVSTEMMING_XML` gjelder for IT28.

## Må gjøres før prod

- [ ] **`azp`-sjekk:** Mottakeren validerer bare issuer og audience og har `allowAllUsers: true`. Valider `azp` mot `AZURE_APP_PRE_AUTHORIZED_APPS`, og krev rolle for client_credentials-token.
- [ ] **Prod-manifest og prod-workflow** finnes ikke ennå. Inbound-regelen må liste `grunn-og-hjelpestonad` i `prod-gcp`.
- [ ] **README:** Token-audience `dev-fss.teamfamilie.familie-oppdrag` er utdatert. Riktig er `api://dev-gcp.grunn-og-hjelp.grunn-og-hjelp-oppdrag/.default`.

## Kontrakt mot grunn-og-hjelpestonad

- Grunn-og-hjelpestonad pinner `familie-kontrakter:felles` til samme versjon som forken: `4.0_20260611084408_a47ed94`. Oppdater begge samtidig.
- Endepunkter vi bruker i POC-en: `POST /api/oppdrag`, `POST /api/status`, `POST /api/simulering/v1`.

## Ikke verifisert

- Testkildene i forken er ikke gjennomsøkt for `IT28`.
- Simuleringsmapperen er ikke lest.
