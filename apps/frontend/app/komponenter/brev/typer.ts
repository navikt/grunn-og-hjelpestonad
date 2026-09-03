export interface Brevmal {
  tittel: string;
  informasjonOmBruker: InformasjonOmBruker;
  fastTekstAvslutning: Tekstbolk[];
}

export interface InformasjonOmBruker {
  navn: string;
  fnr: string;
}

export interface Tekstbolk {
  underoverskrift?: string;
  innhold: string;
}

export type MellomlagretBrev = {
  brevmal: Brevmal | null;
  fritekstbolker: Tekstbolk[];
};
