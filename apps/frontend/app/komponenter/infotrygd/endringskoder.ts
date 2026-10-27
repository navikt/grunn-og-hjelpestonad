// Teksten fra kodetabellen ENDRINGSAARSAK i Infotrygd, slik den står der.
// Se apps/infotrygd/docs/gh-i-infotrygd.md#endringskoder.
const endringskoder: Record<string, string> = {
  AN: "Annullert",
  AS: "Avgang småbarnstillegg",
  B: "Barnetillegg",
  BB: "Barn 18 år",
  E: "Endring i beregningsgrunnlag",
  F: "Førstegangsvedtak",
  G: "G-regulering",
  H: "Barn etterlatte.3 måneder",
  I: "Institusjonsopphold",
  IN: "Ny inntektsgr.",
  KB: "Barn 3 år",
  NB: "Nytt barn",
  NY: "Ny",
  O: "Opphørt",
  S: "Satsendring",
  TS: "Tilgang småbarnstillegg",
  U: "Uføregrad",
  UA: "Uaktuell",
  P: "Passivt",
  AV: "Avslag",
  OO: "Overf ny løsning",
};

export const formaterEndringskode = (kode: string): string => {
  const renset = kode.trim();
  const tekst = endringskoder[renset.toUpperCase()];
  return tekst ? `${tekst} (${renset})` : renset;
};
