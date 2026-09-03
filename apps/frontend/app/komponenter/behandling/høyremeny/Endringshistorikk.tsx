import React from "react";
import { BodyShort, Detail, Skeleton, Tag, Tooltip, VStack } from "@navikt/ds-react";
import { useBehandlingContext } from "~/contexts/BehandlingContext";
import { useHentEndringshistorikk } from "~/hooks/useHentEndringshistorikk";
import { formaterIsoDatoTid, formaterRelativTid } from "~/utils/utils";
import type { BehandlingEndring } from "~/types/endringshistorikk";
import { endringMeta, grupperKonsekutiveEndringer, type EndringGruppe } from "./endringMeta";
import styles from "./Endringshistorikk.module.css";

const EndringRad = ({ endring }: { endring: BehandlingEndring }) => {
  const meta = endringMeta[endring.endringType] ?? {
    tekst: endring.endringType,
    ikon: () => null,
    farge: "neutral" as const,
    erMilepæl: false,
  };
  const Ikon = meta.ikon;

  return (
    <div className={styles.endringRad}>
      <div className={`${styles.tidslinjePunkt} ${styles[`punkt_${meta.farge}`]}`} />
      <div className={styles.endringInnhold}>
        <div className={styles.endringTittel}>
          <Ikon className={`${styles.endringIkon} ${styles[`ikon_${meta.farge}`]}`} aria-hidden />
          {meta.erMilepæl ? (
            <Tag variant="moderate" size="xsmall" data-color={meta.farge}>
              {meta.tekst}
            </Tag>
          ) : (
            <BodyShort size="small" weight="semibold">
              {meta.tekst}
            </BodyShort>
          )}
        </div>
        {endring.detaljer && <Detail textColor="subtle">{endring.detaljer}</Detail>}
        <Tooltip content={formaterIsoDatoTid(endring.utførtTid)} placement="left">
          <Detail textColor="subtle" className={styles.tidspunkt}>
            {formaterRelativTid(endring.utførtTid)}
          </Detail>
        </Tooltip>
      </div>
    </div>
  );
};

const GruppeBlokk = ({ gruppe }: { gruppe: EndringGruppe }) => {
  return (
    <div className={styles.gruppeBlokk}>
      <div className={styles.gruppeHeader}>
        <div className={styles.avatar}>{gruppe.utførtAv.slice(0, 2)}</div>
        <BodyShort size="small" weight="semibold">
          {gruppe.utførtAv}
        </BodyShort>
      </div>
      <div className={styles.gruppeEndringer}>
        {gruppe.endringer.map((endring) => (
          <EndringRad key={endring.id} endring={endring} />
        ))}
      </div>
    </div>
  );
};

export const Endringshistorikk = () => {
  const { behandling } = useBehandlingContext();
  const { endringshistorikk, laster } = useHentEndringshistorikk(behandling?.id);

  if (laster) {
    return (
      <VStack gap="space-8">
        <Skeleton variant="text" width="80%" />
        <Skeleton variant="text" width="60%" />
        <Skeleton variant="text" width="70%" />
      </VStack>
    );
  }

  if (!endringshistorikk || endringshistorikk.length === 0) {
    return (
      <BodyShort size="small" textColor="subtle">
        Ingen endringer registrert
      </BodyShort>
    );
  }

  const grupper = grupperKonsekutiveEndringer(endringshistorikk);

  return (
    <div className={styles.historikkListe}>
      {grupper.map((gruppe, index) => (
        <GruppeBlokk key={index} gruppe={gruppe} />
      ))}
    </div>
  );
};
