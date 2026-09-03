CREATE TABLE behandling_barn (
                                    id                  UUID         PRIMARY KEY,
                                    behandling_id       UUID         NOT NULL,
                                    soknad_barn_id      UUID,
                                    person_ident        VARCHAR,
                                    navn                VARCHAR,
                                    opprettet_av        VARCHAR       NOT NULL,
                                    opprettet_tid       TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,
                                    endret_av           VARCHAR       NOT NULL,
                                    endret_tid          TIMESTAMP(3) NOT NULL DEFAULT localtimestamp,

                                    CONSTRAINT fk_behandling_barn_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id)
);
