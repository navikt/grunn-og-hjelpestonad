import { format, parseISO } from "date-fns";

const maanedsformat = new Intl.DateTimeFormat("nb-NO", {
  month: "long",
  year: "numeric",
  timeZone: "UTC",
});

export const formaterMaaned = (maanedNotert: string): string => {
  const treff = /^(\d{4})-(\d{2})$/.exec(maanedNotert);
  if (!treff) return maanedNotert;

  const aar = Number(treff[1]);
  const maaned = Number(treff[2]);
  if (maaned < 1 || maaned > 12) return maanedNotert;

  try {
    return maanedsformat.format(new Date(Date.UTC(aar, maaned - 1, 1)));
  } catch {
    return maanedNotert;
  }
};

export const formaterDato = (dato: string): string => {
  try {
    return format(parseISO(dato), "dd.MM.yyyy");
  } catch {
    return dato;
  }
};

export const formaterPeriode = (fom: string, tom: string | null | undefined): string =>
  tom ? `${formaterDato(fom)} – ${formaterDato(tom)}` : `${formaterDato(fom)} –`;
