CREATE TABLE IF NOT EXISTS `subordinate`
(
    `subordinate_id`        UUID         NOT NULL DEFAULT (UUID()),
    `ta_im_id`              UUID         NOT NULL,
    `policy_id`             UUID                  DEFAULT NULL,
    `jwks`                  TEXT                  DEFAULT NULL COMMENT 'JWKSet for SubordinateEntity',
    `entityidentifyer`      VARCHAR(255)          DEFAULT NULL COMMENT 'Subject',
    `crit`                  TEXT                  DEFAULT NULL COMMENT 'List of crit claims',
    `metadata_policy_crit`  TEXT                  DEFAULT NULL COMMENT 'List of metadata_policy_crit',
    `ec_location`           VARCHAR(255)          DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
    `ec_location_automatic` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
    `created_date`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`            VARCHAR(255) NOT NULL,
    `last_modified_by`      VARCHAR(255) NOT NULL,
    PRIMARY KEY (`subordinate_id`),
    FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`) ON DELETE RESTRICT,
    FOREIGN KEY (`policy_id`) REFERENCES `policies` (`policy_id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

