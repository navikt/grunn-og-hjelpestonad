-- Diagnosen velges fra ICD-10, samme kodeverk som Infotrygd bruker, i stedet for å skrives som fritekst.
-- Ingenting er i produksjon, så eksisterende vurderinger slettes i stedet for å konverteres.
DELETE FROM vilkar_diagnose;

ALTER TABLE vilkar_diagnose
    DROP CONSTRAINT vilkar_diagnose_ikke_tom,
    DROP COLUMN diagnose,
    ADD COLUMN kode  TEXT NOT NULL,
    ADD COLUMN tekst TEXT NOT NULL,
    ADD CONSTRAINT vilkar_diagnose_kode_ikke_tom
        CHECK (length(btrim(kode)) > 0);
