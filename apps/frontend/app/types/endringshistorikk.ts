export type EndringType =
  | "BEHANDLING_OPPRETTET"
  | "SENDT_TIL_BESLUTTER"
  | "ANGRET_SEND_TIL_BESLUTTER"
  | "VILKÅR_VURDERING_OPPRETTET"
  | "VILKÅR_VURDERING_OPPDATERT"
  | "VEDTAK_LAGRET"
  | "ÅRSAK_LAGRET"
  | "ÅRSAK_OPPDATERT"
  | "BESLUTTER_GODKJENT"
  | "BESLUTTER_UNDERKJENT"
  | "BEHANDLING_HENLAGT";

export interface BehandlingEndring {
  id: string;
  behandlingId: string;
  endringType: EndringType;
  utførtAv: string;
  utførtTid: string;
  detaljer: string | null;
}
