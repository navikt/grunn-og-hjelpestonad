import { useState } from "react";
import {
  Vurdering,
  type Oppholdstype,
  type Unntakshjemmel,
  type VilkårInstitusjonResponse,
} from "~/types/vilkår";
import { useVilkårSkjema } from "../felles/useVilkårSkjema";

export function useInstitusjonVilkårSkjema() {
  const [oppholdstype, settOppholdstype] = useState<Oppholdstype | "">("");
  const [unntakshjemmel, settUnntakshjemmel] = useState<Unntakshjemmel | "">("");
  const fellesSkjema = useVilkårSkjema();

  const åpneNyPeriode = () => {
    settOppholdstype("");
    settUnntakshjemmel("");
    fellesSkjema.åpneNyPeriode();
  };

  const åpneRedigeringAvPeriode = (periode: VilkårInstitusjonResponse) => {
    settOppholdstype(periode.vurdering === Vurdering.JA ? "" : (periode.oppholdstype ?? ""));
    settUnntakshjemmel(periode.vurdering === Vurdering.JA ? "" : (periode.unntakshjemmel ?? ""));
    fellesSkjema.åpneRedigeringAvPeriode(periode);
  };

  const endreVurdering = (vurdering: Vurdering) => {
    if (vurdering === Vurdering.JA) {
      settOppholdstype("");
      settUnntakshjemmel("");
    }
  };

  /** Backend avviser unntakshjemmel uten oppholdstype, så feltet nullstilles sammen. */
  const endreOppholdstype = (ny: Oppholdstype | "") => {
    settOppholdstype(ny);
    if (ny === "") {
      settUnntakshjemmel("");
    }
  };

  return {
    ...fellesSkjema,
    oppholdstype,
    settOppholdstype,
    unntakshjemmel,
    settUnntakshjemmel,
    åpneNyPeriode,
    åpneRedigeringAvPeriode,
    endreVurdering,
    endreOppholdstype,
  };
}
