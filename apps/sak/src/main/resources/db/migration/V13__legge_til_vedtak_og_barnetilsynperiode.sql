CREATE TABLE vedtak (
                     behandling_id          UUID         NOT NULL,
                     resultat_type          VARCHAR      NOT NULL,
                     begrunnelse            VARCHAR,
                     saksbehandler_ident    VARCHAR      NOT NULL,
                     opphor_fom             TIMESTAMP(3) DEFAULT localtimestamp,
                     beslutter_ident        VARCHAR,
                     opprettet_tid          TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
                     opprettet_av           VARCHAR NOT NULL,

                     CONSTRAINT fk_vedtak_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);

CREATE TABLE barnetilsynperiode (
                        behandling_id       UUID         NOT NULL,
                        dato_fra            TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
                        dato_til            TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
                        utgifter            INT NOT NULL,
                        barn                UUID[] NOT NULL,
                        periodetype         VARCHAR NOT NULL,
                        aktivitetstype      VARCHAR,

                        CONSTRAINT fk_barnetilsynperiode_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);
