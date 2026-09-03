import type { Brevmal, InformasjonOmBruker, Tekstbolk } from "~/komponenter/brev/typer";

export const informasjonOmBruker: InformasjonOmBruker = {
  navn: "NAVN PÅ BRUKER",
  fnr: "FNR TIL BRUKER",
};

const fastTekstAvslutning: Tekstbolk[] = [
  {
    underoverskrift: "",
    innhold:
      "Ta kontakt med oss hvis det har skjedd endringer som du er usikker på om du må melde fra om. Du kan kontakte oss på nav.no/kontakt eller på telefon 55 55 33 34.  \n" +
      "Hvis du har fått utbetalt for mye fordi du ikke har meldt fra om at inntekten eller livssituasjonen din har endret seg, må du vanligvis betale tilbake pengene. Det er derfor viktig at du selv følger med på utbetalinger fra Nav og melder fra om eventuelle feil. \n",
  },
  {
    underoverskrift: "Du kan klage på vedtaket",
    innhold:
      "Hvis du mener vedtaket er feil, kan du klage innen [antall uker fylles ut av breveieren] uker fra den datoen vedtaket har kommet fram til deg. Dette følger av [sett inn lovhenvisning]. Du finner skjema og informasjon på nav.no/klage.  \n" +
      "Nav kan veilede deg på telefon om hvordan du sender en klage. Nav-kontoret ditt kan også hjelpe deg med å skrive en klage. Kontakt oss på telefon 55 55 33 33 <34> hvis du trenger hjelp. \n" +
      "Hvis du får medhold i klagen, kan du få dekket vesentlige utgifter som har vært nødvendige for å få endret vedtaket, for eksempel hjelp fra advokat. Du kan ha krav på fri rettshjelp etter rettshjelploven. Du kan få mer informasjon om denne ordningen hos advokater, statsforvalteren eller Nav. \n" +
      "Du kan lese om saksomkostninger i forvaltningsloven § 36. \n" +
      "Hvis du sender klage i posten, må du signere klagen. \n" +
      "Mer informasjon om klagerettigheter finner du på nav.no/klagerettigheter. \n",
  },
  {
    underoverskrift: "Du har rett til innsyn i saken din",
    innhold:
      "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 34. Du kan lese mer om innsynsretten på nav.no/personvernerklaering. ",
  },
  {
    underoverskrift: "Har du spørsmål? ",
    innhold:
      "Du finner mer informasjon på nav.no/barnetilsyn-gjenlevende. \n" +
      "På nav.no/kontakt kan du chatte eller skrive til oss.\n" +
      "Hvis du ikke finner svar på nav.no kan du ringe oss på telefon 55 55 33 34, hverdager 09.00-15.00.\n",
  },
];

export const brevmaler: Brevmal[] = [
  {
    tittel: "brevmalTittel1",
    informasjonOmBruker: informasjonOmBruker,
    fastTekstAvslutning: fastTekstAvslutning,
  },
  {
    tittel: "brevmalTittel2",
    informasjonOmBruker: informasjonOmBruker,
    fastTekstAvslutning: fastTekstAvslutning,
  },
  {
    tittel: "brevmalTittel3",
    informasjonOmBruker: informasjonOmBruker,
    fastTekstAvslutning: fastTekstAvslutning,
  },
];
