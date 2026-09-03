export interface Behandling {
  id: string;
  fagsakId: string;
  forrigeBehandlingId?: string;
  status: "OPPRETTET" | "UTREDES" | "FATTER_VEDTAK" | "IVERKSETTER_VEDTAK" | "FERDIGSTILT";
  sistEndret: string;
  sistEndretAv: string;
  opprettet: string;
  opprettetAv: string;
  resultat: "INNVILGET" | "OPPHØRT" | "AVSLÅTT" | "IKKE_SATT" | "HENLAGT";
}
