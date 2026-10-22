# Grunn- og hjelpestønad i Infotrygd

Bildene under er hentet fra Confluence-siden
[IT34_GH - Databasetabeller](https://confluence.adeo.no/spaces/INFOTRYGD/pages/126681693/IT34_GH+-+Databasetabeller).
Se [README](../README.md) for hvordan appen leser tabellene.

## Systemskisse

![Systemskisse for grunn- og hjelpestønad i Infotrygd](Infotrygd%20GH%20systemskisse.JPG)

## Datamodell for vedtak og stønad

[![ER-diagram for vedtak og stønad i Infotrygd](Infotrygd_GH%20-%20Databasetabeller.jpg)](Infotrygd_GH%20-%20Databasetabeller.jpg)

Diagrammet har rundt 60 tabeller og dekker alle rutinene som bruker
vedtaksbasen, ikke bare GH. Vi replikerer `LOPENR_FNR`, `STONAD`, `VEDTAK`,
`GH`, `ENDRING` og `DELYTELSE`, med prefikset `t_` (se `ExodusTabell`).

### Tabeller vi ikke replikerer

Disse henger på `VEDTAK` og kan være aktuelle for GH:

| Tabell | Innhold |
|--------|---------|
| `DIAGNOSE` | Hoved- og bidiagnose per vedtak (`type_hoved`, `kode_hoved`, `type_bi`, `kode_bi`), med én rad per registrering |
| `BESLUT` | Saksbehandler, godkjenning og enhet for vedtaket |
| `ROLLE` | Personer med en rolle i vedtaket, med periode og trygdetid |
| `GRAD` | Grad per type og periode |

Kodetabellene gir tekst til kodene: `ENDRINGSAARSAK` for `ENDRING.kode`,
`DELYTELSESTYPE`, `SATSTYPE` og `UTBETALINGSTYPE` for `DELYTELSE`,
`OPPHORSGRUNN` for `STONAD.kode_opphor`, `RUTINE` for `kode_rutine`,
`VEDTAKSNIVAA` og `BEREGNINGSTYPE` for `VEDTAK`.
