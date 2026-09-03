import React from "react";
import { HStack } from "@navikt/ds-react";
import { NavLink } from "react-router";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import styles from "./Navbar.module.css";

export type Steg = "Årsak behandling" | "Vilkår" | "Vedtak og beregning" | "Simulering" | "Brev";
export type StegPath =
  | "arsak-behandling"
  | "vilkar"
  | "vedtak-og-beregning"
  | "simulering"
  | "brev";

export type BehandlingSteg = {
  path: StegPath;
  navn: Steg;
  kanStarte?: (ferdigeSteg: Steg[]) => boolean;
};

type BehandlingFanerProps = {
  steg: BehandlingSteg[];
  ferdigeSteg?: Steg[];
};

export const BehandlingFaner: React.FC<BehandlingFanerProps> = ({ steg, ferdigeSteg = [] }) => {
  return (
    <nav className={styles.navbar}>
      <HStack>
        {steg.map((steg) => {
          const kanStarte = steg.kanStarte ? steg.kanStarte(ferdigeSteg) : true;

          return (
            <NavLink
              key={steg.path}
              to={steg.path}
              className={({ isActive }) => {
                let className = styles.link;
                if (isActive) className += ` ${styles.aktiv}`;
                if (!kanStarte) className += ` ${styles.disabled}`;
                return className;
              }}
              onClick={(e) => {
                if (!kanStarte) {
                  e.preventDefault();
                }
              }}
            >
              {/* TODO: Er det riktige tagger? */}
              <HStack>
                {!kanStarte && <PadlockLockedIcon title="a11y-title" fontSize="1.5rem" />}
                {steg.navn}
              </HStack>
            </NavLink>
          );
        })}
      </HStack>
    </nav>
  );
};
