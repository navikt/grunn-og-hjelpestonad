import { Document, Image, Page, Text, usePDF, View } from "@react-pdf/renderer";
import React, { useEffect, useMemo, useState } from "react";
import { Alert, Loader, VStack } from "@navikt/ds-react";
import type { Brevmal, Tekstbolk } from "~/komponenter/brev/typer";
import cssStyles from "./PdfForhåndsvisning.module.css";
import { pdfStiler as styles } from "./pdfStiler";

interface Props {
  brevmal: Brevmal | null;
  fritekstbolker: Tekstbolk[];
}

const TekstAvsnitt = ({ tekst }: { tekst: string }) => {
  const avsnitt = tekst
    .split("\n")
    .map((a) => a.trim())
    .filter((a) => a.length > 0);

  return (
    <>
      {avsnitt.map((a, i) => (
        <Text
          key={i}
          style={i > 0 ? [styles.avsnitt, { marginTop: 16 }] : styles.avsnitt}
          orphans={3}
          widows={3}
        >
          {a}
        </Text>
      ))}
    </>
  );
};

const BrevDokument = ({ brevmal, fritekstbolker }: Props) => {
  const NAV_LOGO_PATH = "/Nav-logo-red-228x63.png";

  return (
    <Document>
      {brevmal && (
        <Page size="A4" style={styles.page} wrap>
          <View style={styles.header}>
            <Image src={NAV_LOGO_PATH} style={styles.logo} />
          </View>

          <View style={styles.infoblokk}>
            <View style={styles.infoRad}>
              <Text style={styles.infoEtikett}>Navn:</Text>
              <Text>{brevmal.informasjonOmBruker.navn}</Text>
            </View>
            <View style={styles.datoRad}>
              <View style={styles.infoRad}>
                <Text style={styles.infoEtikett}>Fødselsnummer:</Text>
                <Text>{brevmal.informasjonOmBruker.fnr}</Text>
              </View>
              <Text>
                {new Date().toLocaleDateString("nb-NO", {
                  day: "2-digit",
                  month: "2-digit",
                  year: "numeric",
                })}
              </Text>
            </View>
          </View>

          <Text style={styles.tittel}>{brevmal.tittel}</Text>

          {fritekstbolker.map((fritekstbolk, index) => {
            const harOverskrift = !!fritekstbolk.underoverskrift?.trim();
            return (
              <View
                key={index}
                style={harOverskrift ? styles.seksjon : [styles.seksjon, { marginTop: 26 }]}
              >
                {harOverskrift && (
                  <Text style={styles.overskrift} minPresenceAhead={20}>
                    {fritekstbolk.underoverskrift}
                  </Text>
                )}
                <TekstAvsnitt tekst={fritekstbolk.innhold} />
              </View>
            );
          })}

          {brevmal.fastTekstAvslutning?.map((fastTekst, index) => {
            const harOverskrift = !!fastTekst.underoverskrift?.trim();
            return (
              <View
                key={index}
                style={harOverskrift ? styles.seksjon : [styles.seksjon, { marginTop: 26 }]}
                wrap={false}
              >
                {harOverskrift && (
                  <Text style={styles.underoverskrift}>{fastTekst.underoverskrift}</Text>
                )}
                <TekstAvsnitt tekst={fastTekst.innhold} />
              </View>
            );
          })}

          <Text
            style={styles.sidenummer}
            render={({ pageNumber, totalPages }) => `Side ${pageNumber} av ${totalPages}`}
            fixed
          />
        </Page>
      )}
    </Document>
  );
};

export const PdfForhåndsvisning = ({ brevmal, fritekstbolker }: Props) => {
  const [debouncedBrevmal, settDebouncedBrevmal] = useState(brevmal);
  const [debouncedFritekstbolker, settDebouncedFritekstbolker] = useState(fritekstbolker);

  useEffect(() => {
    const timer = setTimeout(() => {
      settDebouncedBrevmal(brevmal);
      settDebouncedFritekstbolker(fritekstbolker);
    }, 1000);

    return () => clearTimeout(timer);
  }, [brevmal, fritekstbolker]);

  const dokument = useMemo(
    () => <BrevDokument brevmal={debouncedBrevmal} fritekstbolker={debouncedFritekstbolker} />,
    [debouncedBrevmal, debouncedFritekstbolker]
  );

  const [instans, oppdaterInstans] = usePDF({ document: dokument });

  useEffect(() => {
    oppdaterInstans(dokument);
  }, [dokument, oppdaterInstans]);

  if (typeof window === "undefined") return null;

  if (instans.loading) {
    return (
      <VStack align="center" justify="center" height="100%">
        <Loader size="xlarge" title="Genererer PDF..." />
      </VStack>
    );
  }

  if (instans.error) {
    return <Alert variant="error">Feil ved generering av PDF: {String(instans.error)}</Alert>;
  }

  const url = instans.url ? `${instans.url}#toolbar=0&navpanes=0&view=FitH` : undefined;

  return <iframe src={url} className={cssStyles.pdfIframe} title="Forhåndsvisning av brev" />;
};
