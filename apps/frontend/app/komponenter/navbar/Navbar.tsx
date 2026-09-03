import React from "react";
import { HStack } from "@navikt/ds-react";
import { NavLink } from "react-router";
import styles from "./Navbar.module.css";

type NavLenke = {
  path: string;
  tekst: string;
};

const LENKER: NavLenke[] = [
  { path: "behandlingsoversikt", tekst: "Behandlingsoversikt" },
  { path: "personoversikt", tekst: "Personoversikt" },
  { path: "infotrygd-historikk", tekst: "Infotrygd historikk" },
  { path: "dokumentoversikt", tekst: "Dokumentoversikt" },
];

export const Navbar = () => {
  return (
    <nav className={styles.navbar}>
      <HStack>
        {LENKER.map((lenke) => (
          <NavLink
            key={lenke.path}
            to={lenke.path}
            className={({ isActive }) => `${styles.link}${isActive ? ` ${styles.aktiv}` : ""}`}
          >
            {lenke.tekst}
          </NavLink>
        ))}
      </HStack>
    </nav>
  );
};
