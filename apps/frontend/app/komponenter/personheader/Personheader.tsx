import React from "react";
import styles from "./Personheader.module.css";
import { PersonIcon } from "@navikt/aksel-icons";
import { BodyShort, CopyButton, HStack, Link } from "@navikt/ds-react";
import { usePersonContext } from "~/contexts/PersonContext";
import { beregnAlder, formaterNavn } from "~/utils/utils";

export const Personheader = () => {
  const context = usePersonContext();
  const { personident, fagsakPersonId, person } = context;
  const visningsNavn = person ? formaterNavn(person.navn) : "Navn ikke tilgjengelig";
  const alder = person?.fødselsdato && beregnAlder(person.fødselsdato);

  return (
    <ul className={styles.personheader}>
      <li className={styles.li}>
        <HStack gap="space-2">
          <PersonIcon title="person" fontSize="1.5rem" />
          <BodyShort weight="semibold">
            <Link
              href={`/person/${fagsakPersonId}/behandlingsoversikt`}
            >{`${visningsNavn} ${alder ? `(${alder})` : ""}`}</Link>
          </BodyShort>
        </HStack>
      </li>
      <li className={styles.li}>
        <HStack align="center">
          {personident || "Personident ikke tilgjengelig"}
          {personident && <CopyButton copyText={personident} size="small" />}
        </HStack>
      </li>

      <li id="personheader-actions" className={styles.actions} />
    </ul>
  );
};

export default Personheader;
