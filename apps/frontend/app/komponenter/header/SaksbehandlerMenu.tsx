import React from "react";
import { LeaveIcon } from "@navikt/aksel-icons";
import { BodyShort, Detail, Dropdown, InternalHeader, Spacer, Switch } from "@navikt/ds-react";
import type { Saksbehandler } from "~/server/types";
import { useTemaContext } from "~/contexts/TemaContext";
import styles from "./SaksbehandlerMenu.module.css";

interface SaksbehandlerMenuProps {
  saksbehandler: Saksbehandler | null;
}

export const SaksbehandlerMenu: React.FC<SaksbehandlerMenuProps> = ({ saksbehandler }) => {
  const saksbehandlerNavn = saksbehandler?.navn || "";
  const { mørktTema, byttTema } = useTemaContext();

  return (
    <Dropdown>
      <InternalHeader.UserButton
        as={Dropdown.Toggle}
        name={saksbehandlerNavn}
        description="Enhet: ukjent"
      />

      <Dropdown.Menu className={`${styles.meny} ${mørktTema ? "dark" : "light"}`} data-color="accent">
        <dl>
          <BodyShort as="dt" size="small">
            {saksbehandlerNavn}
          </BodyShort>
          <Detail as="dd">{saksbehandler?.navIdent}</Detail>
        </dl>
        <Dropdown.Menu.Divider />

        <Dropdown.Menu.List>
          <li>
            <div className={styles.rad}>
              <Switch size="small" checked={mørktTema} onChange={byttTema}>
                Mørkt tema
              </Switch>
            </div>
          </li>
          <li>
            <a href="/oauth2/logout" className={styles.rad}>
              Logg ut <Spacer /> <LeaveIcon aria-hidden />
            </a>
          </li>
        </Dropdown.Menu.List>
      </Dropdown.Menu>
    </Dropdown>
  );
};
