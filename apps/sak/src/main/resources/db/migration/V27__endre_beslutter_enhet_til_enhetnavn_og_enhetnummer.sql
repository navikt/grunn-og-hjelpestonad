ALTER TABLE brev
    RENAME COLUMN beslutter_enhet TO beslutter_enhetnavn;

ALTER TABLE brev
    ADD COLUMN beslutter_enhetnummer VARCHAR;
