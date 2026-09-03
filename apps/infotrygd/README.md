# Infotrygd

App som henter ut data fra Infotrygd for gjenlevende med barnetilsyn og skolepenger.
Kjører i `dev-fss` og eksponeres for `apps/sak` via `dev-fss-pub.nais.io`.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/infotrygd`.**

## Bygg og test

```bash
cd apps/infotrygd
mvn verify --settings .m2/maven-settings.xml
```

`GITHUB_USERNAME` og `GITHUB_TOKEN` må være satt for å hente `no.nav`-pakker fra
GitHub Packages.

## Formatering

```bash
mvn antrun:run@ktlint --settings .m2/maven-settings.xml
```

## Datakilde

Appen leser fra Oracle-databasen `infotrygd_ebq`. Credentials hentes fra Vault
(`/oracle/data/dev/creds/eb_infotrygd_q1-user`), se [`.nais/dev.yaml`](.nais/dev.yaml).
