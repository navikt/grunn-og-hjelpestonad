# 27: Sammenslåtte gjeldende perioder med Infotrygd-beløp

**What to build:** Saksbehandler ser gjeldende perioder fra Infotrygd med Infotrygd-beløp, akkurat slik migreringen vil bruke dem. Da kan saksbehandler se i oppslaget om migreringen blir riktig. Periodene slås ikke sammen her, se kommentaren 2026-11-06.

**Blocked by:** 26. Begge endrer reglene for gjeldende perioder.

**Status:** ready-for-human

🔴 Rød sone: regelen for Infotrygd-beløp. Utvikleren skriver den rene funksjonen og testene. Kolonnen i frontend er grønn sone.

Spec: [spec.md](../spec.md). Beslutningene står i [ticket 03](03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md) og ADR-0012.

- [x] Hver gjeldende periode har Infotrygd-beløpet fra grunnstønad-delytelsen på vedtaket.
- [x] Vedtaket avgjør perioden. Delytelsen endrer ikke fom eller tom.
- [x] Dekker delytelsen mindre enn vedtaksperioden etter kuttet ved opphør, i hvilken som helst periode, gir oppslaget `UgyldigInput`.
- [x] Har delytelsen en annen periode enn vedtaket i den siste gjeldende perioden, gir oppslaget samme feil, også når delytelsen dekker mer. I tidligere perioder er det ikke en feil når delytelsen dekker mer.
- [x] Har vedtaket ingen delytelse, gir oppslaget samme feil.
- [x] Feilen har teksten «Vedtak {vedtakId} gjelder {fom}–{tom}, men delytelsen med beløpet i Infotrygd gjelder {delytelseFom}–{delytelseTom}. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.» Er vedtaket eller delytelsen løpende, står det «fra {fom}».
- [x] Har vedtaket flere delytelser, gir oppslaget `UgyldigInput`, også når beløpene er like.
- Flyttet til ticket 14: Sats og trygdetid kommer fra raden i `t_gh` med nyest registreringstidspunkt.
- Flyttet til ticket 14: Perioder som følger etter hverandre, slås sammen når Infotrygd-beløp, sats, trygdetid, diagnose og hjemmelbokstav er like. En tom verdi regnes som en egen verdi.
- [x] «Gjeldende perioder» i frontend har en kolonne for beløp.
- [x] Rene tester dekker: delytelse som slutter før vedtaket, delytelse som starter etter vedtaket, delytelse som varer lenger enn vedtaket i en tidligere periode og i den siste perioden, vedtak uten delytelse og vedtak med flere delytelser. Testene for sammenslåing og arvet `t_gh` er flyttet til ticket 14.
- [ ] Teksten i feilmeldingen er gjennomgått av `@forfatter`.

## Comments

**2026-11-02:** Opphør den 1. i måneden er første dag uten stønad, som i EF, se «Status» i kommentaren 2026-11-02 i ticket 03. Perioden slutter da siste dag i måneden før, altså ved et månedsskifte, så ticket 27 trenger ingen egen regel for en del av en måned.

**2026-11-06:** Avklart før implementering:

- «Gjeldende perioder» slås ikke sammen. Hver periode har fortsatt ett vedtak, slik at saksbehandler ser hvilket vedtak som gjelder. Sammenslåingen og regelen for sats og trygdetid fra `t_gh` hører til valideringen i ticket 14.
- Delytelsen sammenlignes med hele vedtaket etter kuttet ved opphør, ikke med delen av vedtaket som gjelder i tidslinjen. Bare vedtak som er med i minst én gjeldende periode, sjekkes. Et vedtak som er helt overskrevet av et nyere, gir ingen feil.
- Et vedtak med flere delytelser gir feil, også når beløpene er like. Teksten er «Vedtak {vedtakId} har {antall} delytelser i Infotrygd, ikke én. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.» Et vedtak uten delytelse gir «Vedtak {vedtakId} gjelder {fom}–{tom}, men har ingen delytelse med beløpet i Infotrygd. Vi har ikke bestemt hvordan slike vedtak skal tolkes. Kontakt teamet.»
- Koden filtrerer ikke på `type_delytelse`. `/stonader` gir én stønadstype, og alle delytelsene har da samme type: `MG` for grunnstønad og `MH` for hjelpestønad.

**2026-11-06:** Målt i dev: delytelser som slutter senere enn vedtaket i siste periode. Resultatet står under «Delytelsen i siste periode» i ADR-0012.

| Kolonne | Antall |
|---|---:|
| `lopende` (siste periode løpende eller slutter på eller etter 2026-01-01) | 147 482 |
| `lopende_med_avvik` | 1 818 |
| `lopende_i_dag` | 144 672 |
| `lopende_i_dag_med_avvik` | 0 |
| `kuttet_ved_opphor` av de 1 818 | 1 818 |

Spørringen kjøres mot replikaen i `apps/infotrygd`. Den følger reglene i `GjeldendeInfotrygdPerioder`, men tar ikke ut personer som får en annen feil først. Siste periode er den høyeste tom etter kuttet, og av vedtakene som slutter da, vinner høyest `vedtak_id`. Svaret har bare antall.

```sql
with vedtak as (
    select v.person_lopenr,
           v.vedtak_id,
           v.dato_innv_tom as tom_innv,
           -- tom etter kuttet ved opphør. Opphør den 1. betyr at siste dag med stønad er dagen før.
           -- least() hopper over null, så et løpende vedtak på en løpende stønad får null.
           least(
               v.dato_innv_tom,
               case when extract(day from s.dato_opphor) = 1 then s.dato_opphor - 1 else s.dato_opphor end
           ) as tom
    from t_vedtak v
    join t_stonad s on s.stonad_id = v.stonad_id and s.person_lopenr = v.person_lopenr
    where s.kode_rutine = 'GS'
      and exists (select 1 from t_endring e where e.vedtak_id = v.vedtak_id and e.kode not in ('AN', 'UA'))
      and v.dato_innv_fom is distinct from s.dato_opphor
),
siste_periode as (
    select distinct on (person_lopenr) *
    from vedtak
    order by person_lopenr, tom desc nulls first, vedtak_id desc
),
med_avvik as (
    select p.*,
           exists (
               select 1 from t_delytelse d
               where d.vedtak_id = p.vedtak_id
                 and p.tom is not null
                 and (d.tom is null or d.tom > p.tom)
           ) as delytelse_slutter_senere
    from siste_periode p
)
select
    count(*) filter (where tom is null or tom >= date '2026-01-01')                    as lopende,
    count(*) filter (where tom >= date '2026-01-01' and delytelse_slutter_senere)       as lopende_med_avvik,
    count(*) filter (where tom is null or tom >= current_date)                         as lopende_i_dag,
    count(*) filter (where tom >= current_date and delytelse_slutter_senere)            as lopende_i_dag_med_avvik,
    count(*) filter (where tom >= date '2026-01-01' and delytelse_slutter_senere
                     and (tom_innv is null or tom < tom_innv))                          as kuttet_ved_opphor
from med_avvik;
```
