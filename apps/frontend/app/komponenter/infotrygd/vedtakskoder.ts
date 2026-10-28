// Teksten fra kodetabellen i Infotrygd (KT1_TEKSTTYPE T for sakstype og R for resultat), slik den står der.
// Se apps/infotrygd/docs/gh-i-infotrygd.md#sakstyper-og-resultatkoder.
import { formaterKode } from "./endringskoder";

const sakstyper: Record<string, string> = {
  A: "Anke",
  FU: "Flytting til utlandet",
  K: "Klage",
  KO: "Konverteringssak",
  MO: "Manuell satsomregning",
  MS: "Maskinell satsomregning",
  R: "Revurdering",
  S: "Søknad",
  SØ: "Søknad om økning/endring",
};

const resultater: Record<string, string> = {
  A: "Avslag",
  AN: "Annulert",
  DI: "Delvis innvilget",
  FI: "Fortsatt innvilget",
  HB: "Henlagt / bortfalt",
  I: "Innvilget",
  IN: "Innvilget ny situasjon",
  K: "Konvertert",
  MO: "Midlertidig opphørt",
  R: "Redusert",
  Ø: "Økning",
};

// Ekstrautgiftene i folketrygdloven § 6-3 første ledd, forkortet. Bokstav d og e ble opphevet
// fra 1. januar 2020 (lov 20. desember 2019 nr. 84), men finnes i eldre vedtak. Teksten for d er
// fra lov 15. juni 2001 nr. 90, og e er fra lov 20. desember 2018 nr. 98.
const hjemmelbokstaver: Record<string, string> = {
  A: "Drift av tekniske hjelpemidler",
  B: "Transport, herunder drift av medlemmets bil",
  C: "Hold av førerhund",
  D: "Teksttelefon og i særlige tilfeller vanlig telefon",
  E: "Bruk av proteser, støttebandasje o.l.",
  F: "Fordyret kosthold ved nødvendig spesialdiett",
  G: "Slitasje på klær og sengetøy",
  H: "Hold av servicehund",
};

export const formaterSakstype = (kode: string): string => formaterKode(sakstyper, kode);

export const formaterHjemmelbokstav = (kode: string): string =>
  formaterKode(hjemmelbokstaver, kode);

export const formaterResultat = (kode: string): string => formaterKode(resultater, kode);
