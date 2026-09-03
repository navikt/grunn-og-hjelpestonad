export enum TotrinnskontrollStatus {
  IKKE_AUTORISERT = "IKKE_AUTORISERT",
  TOTRINNSKONTROLL_UNDERKJENT = "TOTRINNSKONTROLL_UNDERKJENT",
  UAKTUELT = "UAKTUELT",
  KAN_FATTE_VEDTAK = "KAN_FATTE_VEDTAK",
}

export enum ÅrsakUnderkjent {
  ÅRSAK_BEHANDLING = "ÅRSAK_BEHANDLING",
  VILKÅR = "VILKÅR",
  VEDTAK_OG_BEREGNING = "VEDTAK_OG_BEREGNING",
  SIMULERING = "SIMULERING",
  BREV = "BREV",
  RETUR_ETTER_EGET_ØNSKE = "RETUR_ETTER_EGET_ØNSKE",
}

export const årsakUnderkjentTekst: Record<ÅrsakUnderkjent, string> = {
  [ÅrsakUnderkjent.ÅRSAK_BEHANDLING]: "Årsak behandling",
  [ÅrsakUnderkjent.VILKÅR]: "Vilkår",
  [ÅrsakUnderkjent.VEDTAK_OG_BEREGNING]: "Vedtak og beregning",
  [ÅrsakUnderkjent.SIMULERING]: "Simulering",
  [ÅrsakUnderkjent.BREV]: "Brev",
  [ÅrsakUnderkjent.RETUR_ETTER_EGET_ØNSKE]: "Retur etter eget ønske",
};

export type TotrinnskontrollResponse =
  | {
      status: TotrinnskontrollStatus.IKKE_AUTORISERT;
      totrinnskontroll: TotrinnskontrollOpprettet;
    }
  | {
      status: TotrinnskontrollStatus.TOTRINNSKONTROLL_UNDERKJENT;
      totrinnskontroll: TotrinnskontrollUnderkjentDetaljer;
    }
  | {
      status: TotrinnskontrollStatus.KAN_FATTE_VEDTAK | TotrinnskontrollStatus.UAKTUELT;
    };

export type TotrinnskontrollOpprettet = {
  opprettetAv: string;
  opprettetTid: string;
};

export type TotrinnskontrollUnderkjentDetaljer = TotrinnskontrollOpprettet & {
  årsakUnderkjent?: ÅrsakUnderkjent;
  begrunnelse?: string;
};
