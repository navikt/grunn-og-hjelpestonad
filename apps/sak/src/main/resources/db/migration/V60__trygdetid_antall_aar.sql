-- Tallet er trygdetiden i hele år, som i folketrygdloven § 6-6, ikke en teller i en brøk.
ALTER TABLE trygdetid RENAME COLUMN teller TO antall_aar;
ALTER TABLE trygdetid RENAME CONSTRAINT trygdetid_teller_omraade TO trygdetid_antall_aar_omraade;
