import React from "react";
import { HStack } from "@navikt/ds-react";
import { NavLink } from "react-router";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import styles from "./Navbar.module.css";
import { erFaneLåst, type BehandlingForFaner, type GjeldendeSteg } from "./behandlingFaneRegel";

export type Steg =
  "Årsak behandling" | "Inngangsvilkår" | "Sats og trygdetid" | "Simulering" | "Brev";
export type StegPath = "arsak-behandling" | "vilkar" | "sats-og-trygdetid" | "simulering" | "brev";

export type BehandlingSteg = {
  path: StegPath;
  navn: Steg;
  behandlingssteg: GjeldendeSteg;
};

type BehandlingFanerProps = {
  steg: BehandlingSteg[];
  behandling?: BehandlingForFaner;
};

export const BehandlingFaner: React.FC<BehandlingFanerProps> = ({ steg, behandling }) => {
  return (
    <nav className={styles.navbar}>
      <HStack>
        {steg.map((steg) => {
          const kanStarte = !erFaneLåst(steg.behandlingssteg, behandling);

          return (
            <NavLink
              key={steg.path}
              to={steg.path}
              className={({ isActive }) =>
                [styles.link, isActive && styles.aktiv, !kanStarte && styles.disabled]
                  .filter(Boolean)
                  .join(" ")
              }
              onClick={(e) => {
                if (!kanStarte) {
                  e.preventDefault();
                }
              }}
            >
              {/* TODO: Er det riktige tagger? */}
              <HStack>
                {!kanStarte && <PadlockLockedIcon title="Låst" fontSize="1.5rem" />}
                {steg.navn}
              </HStack>
            </NavLink>
          );
        })}
      </HStack>
    </nav>
  );
};
