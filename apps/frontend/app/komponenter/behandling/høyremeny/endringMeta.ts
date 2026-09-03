import type React from "react";
import {
  PlusCircleIcon,
  PaperplaneIcon,
  ArrowUndoIcon,
  TasklistIcon,
  CheckmarkCircleIcon,
  NotePencilIcon,
  XMarkOctagonIcon,
} from "@navikt/aksel-icons";
import type { EndringType, BehandlingEndring } from "~/types/endringshistorikk";
import type { TagFarge } from "~/types/farge";

export interface EndringMeta {
  tekst: string;
  ikon: React.ElementType;
  farge: TagFarge;
  erMilepæl: boolean;
}

export const endringMeta: Record<EndringType, EndringMeta> = {
  BEHANDLING_OPPRETTET: {
    tekst: "Behandling opprettet",
    ikon: PlusCircleIcon,
    farge: "success",
    erMilepæl: true,
  },
  SENDT_TIL_BESLUTTER: {
    tekst: "Sendt til beslutter",
    ikon: PaperplaneIcon,
    farge: "info",
    erMilepæl: true,
  },
  ANGRET_SEND_TIL_BESLUTTER: {
    tekst: "Angret send til beslutter",
    ikon: ArrowUndoIcon,
    farge: "warning",
    erMilepæl: true,
  },
  VILKÅR_VURDERING_OPPRETTET: {
    tekst: "Vilkår opprettet",
    ikon: TasklistIcon,
    farge: "info",
    erMilepæl: false,
  },
  VILKÅR_VURDERING_OPPDATERT: {
    tekst: "Vilkår oppdatert",
    ikon: TasklistIcon,
    farge: "info",
    erMilepæl: false,
  },
  VEDTAK_LAGRET: {
    tekst: "Vedtak lagret",
    ikon: CheckmarkCircleIcon,
    farge: "success",
    erMilepæl: false,
  },
  ÅRSAK_LAGRET: {
    tekst: "Årsak lagret",
    ikon: NotePencilIcon,
    farge: "info",
    erMilepæl: false,
  },
  ÅRSAK_OPPDATERT: {
    tekst: "Årsak oppdatert",
    ikon: NotePencilIcon,
    farge: "info",
    erMilepæl: false,
  },
  BESLUTTER_GODKJENT: {
    tekst: "Beslutter godkjent",
    ikon: CheckmarkCircleIcon,
    farge: "success",
    erMilepæl: true,
  },
  BESLUTTER_UNDERKJENT: {
    tekst: "Beslutter underkjent",
    ikon: XMarkOctagonIcon,
    farge: "warning",
    erMilepæl: true,
  },
  BEHANDLING_HENLAGT: {
    tekst: "Behandling henlagt",
    ikon: XMarkOctagonIcon,
    farge: "danger",
    erMilepæl: true,
  },
};

export interface EndringGruppe {
  utførtAv: string;
  endringer: BehandlingEndring[];
}

export const grupperKonsekutiveEndringer = (endringer: BehandlingEndring[]): EndringGruppe[] => {
  const grupper: EndringGruppe[] = [];

  for (const endring of endringer) {
    const sisteGruppe = grupper[grupper.length - 1];

    if (sisteGruppe && sisteGruppe.utførtAv === endring.utførtAv) {
      sisteGruppe.endringer.push(endring);
    } else {
      grupper.push({ utførtAv: endring.utførtAv, endringer: [endring] });
    }
  }

  return grupper;
};
