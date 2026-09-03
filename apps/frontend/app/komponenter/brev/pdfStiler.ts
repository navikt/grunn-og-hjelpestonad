import { StyleSheet } from "@react-pdf/renderer";

export const pdfStiler = StyleSheet.create({
  page: {
    paddingTop: 64,
    paddingBottom: 74,
    paddingHorizontal: 64,
    fontSize: 11,
    lineHeight: 16 / 11,
    color: "#000000",
  },
  header: {
    marginBottom: 48,
  },
  logo: {
    height: 16,
    alignSelf: "flex-start",
    objectFit: "contain",
  },
  infoblokk: {
    marginBottom: 26,
  },
  infoRad: {
    flexDirection: "row",
  },
  infoEtikett: {
    width: 110,
  },
  datoRad: {
    flexDirection: "row",
    justifyContent: "space-between",
  },
  tittel: {
    fontSize: 16,
    lineHeight: 20 / 16,
    fontWeight: "bold",
    letterSpacing: 0.3,
    marginBottom: 26,
  },
  seksjon: {
    marginBottom: 0,
  },
  overskrift: {
    fontSize: 13,
    lineHeight: 16 / 13,
    fontWeight: "bold",
    letterSpacing: 0.25,
    marginTop: 26,
    marginBottom: 6,
  },
  underoverskrift: {
    fontSize: 12,
    lineHeight: 16 / 12,
    fontWeight: "bold",
    letterSpacing: 0.2,
    marginTop: 26,
    marginBottom: 6,
  },
  avsnitt: {
    marginBottom: 0,
  },
  sidenummer: {
    position: "absolute",
    bottom: 26,
    left: 64,
    right: 64,
    fontSize: 9,
    textAlign: "right",
    color: "#000000",
  },
});
