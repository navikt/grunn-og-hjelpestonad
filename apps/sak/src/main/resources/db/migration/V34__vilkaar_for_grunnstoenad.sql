-- Vilkårstypene er arvet fra forken (gjenlevende/barnetilsyn) og har ingen hjemmel i
-- grunnstønad etter folketrygdloven kapittel 6. En omforming til de nye vilkårene ville
-- gitt rettslig meningsløse vurderinger, så de gamle radene slettes. Applikasjonen kjører
-- kun i dev-gcp, og det finnes ingen produksjonsdata.
DELETE FROM vilkar_vurdering;

-- IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM er 51 tegn og får ikke plass i
-- VARCHAR(50) fra V18.
ALTER TABLE vilkar_vurdering
    ALTER COLUMN vilkar_type TYPE TEXT;

-- Gjør det mulig for vilkar_diagnose å peke på (id, vilkar_type) i stedet for bare id.
-- Dette er ikke en unique-constraint på (behandling_id, vilkar_type); flere perioder per
-- vilkårstype er fortsatt tillatt, jf. ADR-0003.
ALTER TABLE vilkar_vurdering
    ADD CONSTRAINT vilkar_vurdering_id_vilkar_type_unik UNIQUE (id, vilkar_type);

CREATE TABLE vilkar_diagnose
(
    id                  UUID         PRIMARY KEY,
    vilkar_vurdering_id UUID         NOT NULL,
    -- Settes av databasen, ikke av applikasjonen. Kolonnen finnes kun for å la
    -- fremmednøkkelen og CHECK-en nedenfor håndheve at en diagnose bare kan henge på en
    -- vurdering av VARIG_SYKDOM_SKADE_ELLER_LYTE. ADR-0003 krever at gyldighet per vilkår
    -- sikres av databasestruktur og ikke av runtime-validering.
    vilkar_type         TEXT         NOT NULL DEFAULT 'VARIG_SYKDOM_SKADE_ELLER_LYTE',
    diagnose            TEXT         NOT NULL,
    yrkesskade          BOOLEAN      NOT NULL DEFAULT FALSE,
    yrkesskade_dato     DATE,
    opprettet_av        VARCHAR(255) NOT NULL,
    opprettet_tid       TIMESTAMP(3) NOT NULL,
    endret_av           VARCHAR(255) NOT NULL,
    endret_tid          TIMESTAMP(3) NOT NULL,
    CONSTRAINT vilkar_diagnose_kun_varig_sykdom
        CHECK (vilkar_type = 'VARIG_SYKDOM_SKADE_ELLER_LYTE'),
    CONSTRAINT vilkar_diagnose_vilkar_vurdering_fk
        FOREIGN KEY (vilkar_vurdering_id, vilkar_type)
            REFERENCES vilkar_vurdering (id, vilkar_type) ON DELETE CASCADE
);

CREATE INDEX idx_vilkar_diagnose_vilkar_vurdering_id ON vilkar_diagnose (vilkar_vurdering_id);
