import type { OpplysningResponse } from "~/api/generated/types.gen";
import { formaterDato } from "../../infotrygd/formatering";

export type DødsfallVarselTekst = {
  tittel: string;
  beskrivelse: string;
};

export function lagDødsfallVarselTekst(
  dødsfallOpplysninger: ReadonlyArray<OpplysningResponse>
): DødsfallVarselTekst | undefined {
  if (dødsfallOpplysninger.length === 0) return undefined;

  const unikeFormaterteDatoer = [
    ...new Set(
      dødsfallOpplysninger
        .map((dødsfallOpplysning) => dødsfallOpplysning.fraOgMedDato)
        .filter((fraOgMedDato): fraOgMedDato is string => Boolean(fraOgMedDato))
        .map((fraOgMedDato) => formaterDato(fraOgMedDato))
    ),
  ];

  return {
    tittel: "Personen er registrert død",
    beskrivelse: lagBeskrivelse(unikeFormaterteDatoer),
  };
}

function lagBeskrivelse(unikeFormaterteDatoer: string[]): string {
  if (unikeFormaterteDatoer.length === 0) {
    return "Folkeregisteret har registrert dødsfall, men ikke dato. Avslutt medlemskapsperioden når du har funnet dødsdatoen.";
  }
  if (unikeFormaterteDatoer.length === 1) {
    return `Folkeregisteret viser dødsdato ${unikeFormaterteDatoer[0]}. Avslutt medlemskapsperioden på dødsdatoen.`;
  }
  return `Folkeregisteret viser flere dødsdatoer: ${unikeFormaterteDatoer.join(", ")}. Kontroller hvilken som gjelder, og avslutt medlemskapsperioden på dødsdatoen.`;
}
