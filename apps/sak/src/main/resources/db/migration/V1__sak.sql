CREATE TABLE sak (
                     id             uuid PRIMARY KEY,
                     soknad         VARCHAR        NOT NULL,
                     saksnummer     VARCHAR      NOT NULL,
                     opprettet_tid  TIMESTAMP(3) NOT NULL DEFAULT localtimestamp
);
