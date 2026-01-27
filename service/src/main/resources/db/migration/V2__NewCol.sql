ALTER TABLE `entities`
    ADD COLUMN `ec_location` VARCHAR(255) DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
    ADD COLUMN `authorityhints` TEXT DEFAULT NULL
COMMENT
'Authority hints',
    ADD COLUMN `ec_location_automatic` TINYINT(1) NOT NULL DEFAULT 0
COMMENT
'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
    ADD COLUMN `trustmarksources` TEXT DEFAULT NULL COMMENT
'Trustmark sources that can be used to include trustmarks';


