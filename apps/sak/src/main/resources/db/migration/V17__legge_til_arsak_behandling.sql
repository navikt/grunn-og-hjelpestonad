CREATE TABLE arsak_behandling (
                                  behandling_id UUID PRIMARY KEY
                                      REFERENCES behandling(id)
                                          ON DELETE CASCADE,

                                  kravdato DATE NOT NULL,
                                  arsak VARCHAR(50) NOT NULL,
                                  beskrivelse TEXT NOT NULL DEFAULT ''
);
