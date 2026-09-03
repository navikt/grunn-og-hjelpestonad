export type SaksbehandlerRolle =
  | "INNLOGGET_SAKSBEHANDLER"
  | "ANNEN_SAKSBEHANDLER"
  | "IKKE_SATT";

export interface AnsvarligSaksbehandlerDto {
  fornavn: string;
  etternavn: string;
  rolle: SaksbehandlerRolle;
}
