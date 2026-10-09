import type { DiagnosekodeResponse } from "~/types/vilkår";

/** Kodeverket lagrer ICD-10-koder uten punktum (E109), mens leger skriver dem med (E10.9). */
export function formatertKode(kode: string): string {
  return kode.length > 3 ? `${kode.slice(0, 3)}.${kode.slice(3)}` : kode;
}

export function visningAvDiagnosekode({ kode, tekst }: DiagnosekodeResponse): string {
  return `${formatertKode(kode)} ${tekst}`;
}
