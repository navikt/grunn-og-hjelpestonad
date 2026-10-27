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

export const formaterSakstype = (kode: string): string => formaterKode(sakstyper, kode);

export const formaterResultat = (kode: string): string => formaterKode(resultater, kode);
