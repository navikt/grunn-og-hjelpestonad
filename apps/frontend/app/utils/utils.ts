import { format, parseISO } from "date-fns";
import type { Navn } from "~/contexts/PersonContext";

export const formaterNavn = (navn: Navn): string => {
  const deler = [navn.fornavn, navn.mellomnavn, navn.etternavn]
    .filter((del): del is string => Boolean(del))
    .map((del) => del.charAt(0).toUpperCase() + del.slice(1).toLowerCase());
  return deler.join(" ");
};

export const erGyldigPersonident = (verdi: string): boolean => /^\d{11}$/.test(verdi.trim());

export const erGyldigFagsakPersonId = (verdi: string): boolean =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(verdi.trim());

export const erGyldigSøkestreng = (søkestreng: string): boolean => {
  return erGyldigPersonident(søkestreng) || erGyldigFagsakPersonId(søkestreng);
};

export const formaterIsoDatoTid = (dato: string): string => {
  return format(parseISO(dato), "dd.MM.yyyy 'kl'.HH:mm");
};

const replaceUnderscoreWithSpace = (str: string): string => str.split("_").join(" ");

export const toTitleCase = (str: string): string =>
  str.replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase());

export const formatterEnumVerdi = (str: string): string =>
  replaceUnderscoreWithSpace(toTitleCase(str));

export const månedStringTilYearMonth = (value: string | undefined): string => {
  if (!value?.trim()) return "";

  const månederStrings = [
    "januar",
    "februar",
    "mars",
    "april",
    "mai",
    "juni",
    "juli",
    "august",
    "september",
    "oktober",
    "november",
    "desember",
  ];

  const [månedNavn, årString] = value.trim().split(" ");
  const måned = månederStrings.indexOf(månedNavn?.toLowerCase());
  const year = Number(årString);

  if (måned >= 0 && year >= 1000 && year <= 9999) {
    const mm = String(måned + 1).padStart(2, "0");
    return `${year}-${mm}`;
  }

  return "";
};

export const formaterYearMonthStringTilNorskDato = (dateString: string | undefined): string => {
  if (!dateString?.trim()) return "";

  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "";

    return date.toLocaleString("nb-NO", { month: "long", year: "numeric" });
  } catch {
    return "";
  }
};

export function beregnAlder(fødselsdato: string): number;
export function beregnAlder(fødselsdato: string | null | undefined): number | null;
export function beregnAlder(fødselsdato: string | null | undefined): number | null {
  if (!fødselsdato) return null;
  const fødselsDate = new Date(fødselsdato);
  if (isNaN(fødselsDate.getTime())) return null;
  const idag = new Date();
  let alder = idag.getFullYear() - fødselsDate.getFullYear();
  const månedDiff = idag.getMonth() - fødselsDate.getMonth();
  if (månedDiff < 0 || (månedDiff === 0 && idag.getDate() < fødselsDate.getDate())) {
    alder--;
  }
  return alder;
}

export const formaterBelop = (beløp: number): string =>
  new Intl.NumberFormat("nb-NO").format(beløp) + " kr";

export const formaterIsoMånedÅr = (dato: string): string => {
  const [år, måned] = dato.split("-");
  return new Date(Number(år), Number(måned) - 1, 1).toLocaleString("nb-NO", {
    month: "long",
    year: "numeric",
  });
};

export const formaterRelativTid = (isoTid: string): string => {
  const tid = new Date(isoTid);
  const nå = new Date();
  const diffSekunder = Math.floor((nå.getTime() - tid.getTime()) / 1000);

  if (diffSekunder < 60) return "Akkurat nå";
  if (diffSekunder < 3600) return `${Math.floor(diffSekunder / 60)} min. siden`;
  if (diffSekunder < 86400) return `${Math.floor(diffSekunder / 3600)} t. siden`;
  if (diffSekunder < 604800) return `${Math.floor(diffSekunder / 86400)} d. siden`;
  return formaterIsoDatoTid(isoTid);
};
