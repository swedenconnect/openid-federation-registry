![Logo](../docs/images/sweden-connect.png)

# Application Configuration

This document describes the configuration settings available for the OpenID Federation Entity Registry Service.

## Table of Contents

- [Server Configuration](#server-configuration)
- [Spring Application Configuration](#spring-application-configuration)
- [Database Configuration](#database-configuration)
- [Security Configuration](#security-configuration)
- [Logging Configuration](#logging-configuration)
- [Credential Bundles](#credential-bundles)
- [Audit Logging](#audit-logging)
- [OpenID Federation Registry Settings](#openid-federation-registry-settings)

---

## Server Configuration

| Setting                                     | Example Value | Description                                               |
|---------------------------------------------|---------------|-----------------------------------------------------------|
| `server.port`                               | 8010          | Port the server will run on.                              |
| `server.compression.enabled`                | true          | Enables response compression for the server. Recommended. |
| `management.endpoints.web.exposure.include` | `*`           | Configures the exposure of all management endpoints.      |

## Spring Application Configuration

| Setting                          | Example Value         | Description                         |
|----------------------------------|-----------------------|-------------------------------------|
| `spring.application.name`        | EntityRegistryService | The name of the Spring application. |
| `spring.threads.virtual.enabled` | true                  | Enables virtual thread support.     |

## Database Configuration

### Spring DataSource

| Setting                      | Example Value                 | Description                     |
|------------------------------|-------------------------------|---------------------------------|
| `spring.datasource.url`      | jdbc:mariadb://db:3306/testdb | URL of the database connection. |
| `spring.datasource.username` | test                          | Username for the database.      |
| `spring.datasource.password` | test                          | Password for the database.      |

### Spring Flyway

| Setting                 | Example Value | Description                        |
|-------------------------|---------------|------------------------------------|
| `spring.flyway.enabled` | true          | Enables Flyway database migration. |

### Spring JPA

| Setting                                      | Example Value                        | Description                                                                                                      |
|----------------------------------------------|--------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `spring.jpa.database-platform`               | org.hibernate.dialect.MariaDBDialect | Specifies the JPA database platform (MariaDB dialect).                                                           |
| `spring.jpa.hibernate.ddl-auto`              | none                                 | Configures Hibernate's schema generation strategy. None is recommended since Flyway handles database migrations. |
| `spring.jpa.show-sql`                        | true                                 | Enables logging of SQL queries executed by JPA.                                                                  |
| `spring.jpa.properties.hibernate.format_sql` | true                                 | Formats the logged SQL queries.                                                                                  |

## Security Configuration

| Setting                                                         | Example Value        | Description                                             |
|-----------------------------------------------------------------|----------------------|---------------------------------------------------------|
| `spring.security.oauth2.resourceserver.jwt.public-key-location` | classpath:my-key.pub | Location of the public key for the JWT resource server. |

## Logging Configuration

| Setting                               | Example Value | Description                                                     |
|---------------------------------------|---------------|-----------------------------------------------------------------|
| `logging.level.se.swedenconnect.oidf` | debug         | Sets the logging level for the `se.swedenconnect.oidf` package. |

## Credential Bundles

| Setting                                                 | Example Value              | Description                                                       |
|---------------------------------------------------------|----------------------------|-------------------------------------------------------------------|
| `credential.bundles.keystore.signkey.location`          | file:config/local/rsa1.jks | Path to the keystore for the signing key.                         |
| `credential.bundles.keystore.signkey.password`          | Test1234                   | Password for the signing key keystore.                            |
| `credential.bundles.keystore.signkey.type`              | JKS                        | Specifies the type of the keystore (JKS).                         |
| `credential.bundles.jks.federationapi.store-reference`  | signkey                    | References the signing key keystore for the Federation API.       |
| `credential.bundles.jks.federationapi.name`             | FederationAPI-JWKSignkey   | Name of the Federation API signing key.                           |
| `credential.bundles.jks.federationapi.key.alias`        | rsa1                       | Alias of the key used for signing in the Federation API keystore. |
| `credential.bundles.jks.federationapi.key.key-password` | Test1234                   | Password for the signing key in the Federation API keystore.      |

## Audit Logging

| Setting                  | Example Value  | Description                                                                                                                                      |
|--------------------------|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `audit-logging.loglevel` | INFO (default) | Configures the log level for audit logging. Supported values are `INFO`, `DEBUG`, `TRACE`, or `NONE`. If no value is set, it defaults to `INFO`. |

## OpenID Federation Registry Settings

### Federation Service API

| Setting                                                                          | Example Value                           | Description                                                                                |
|----------------------------------------------------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------|
| `openid.federation.registry.federation_service_api.issuer`                       | `http://oidf-registry.swedenconnect.se` | Issuer URI set on JWT from Federation Service API.                                         |
| `openid.federation.registry.federation_service_api.sign-key-alias`               | `federationapi`                         | Key alias used for signing operations in the Federation Service API.                       |
| `openid.federation.registry.federation_service_api.kid-algorithm`                | `serial`                                | Kid algorithm used when generating kid in response in federationapi, can be set to: serial |
| `openid.federation.registry.federation_service_api.token-expiry-duration`        | `PT1H`                                  | Token ExpiryDuration for all tokens delivered from federation endpoint.                    |
| `openid.federation.registry.federation_service_api.notification-active`          | `false`                                 | Flag indicating whether notifications are enabled.                                         |
| `openid.federation.registry.federation_service_api.notification-trust-key-alias` | `trustKey`                              | Alias to spring trust-bundle for outgoing notification requests.                           |
| `openid.federation.registry.federation_service_api.notifications[0].endpoint`    | `https://example.com/notify`            | The URI of the notification endpoint.                                                      |
| `openid.federation.registry.federation_service_api.notifications[0].instance_id` | `UUID`                                  | The unique identifier for the instance sending notifications.                              |

### Federation Instances

Defines individual instances under the OpenID Federation Registry.

| Setting                                                              | Example Value                          | Description                                                                       |
|----------------------------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------|
| `openid.federation.registry.instances[0].instance_id`                | `123e4567-e89b-12d3-a456-426614174000` | Unique identifier for the first Federation Instance, representing Sweden Connect. |
| `openid.federation.registry.instances[0].name`                       | `Swedenconnect`                        | Name of the first Federation Instance, representing Sweden Connect.               |
| `openid.federation.registry.instances[0].use-for-default-assignment` | `false`                                | Flag indicating if this instance should be used for the default assignment.       |
| `openid.federation.registry.instances[0].org_numbers`                | `123456-7890`                          | A list of organizational numbers associated with the instance.                    |
| `openid.federation.registry.instances[1].instance_id`                | `223e4567-e89b-12d3-a456-426614174001` | Unique identifier for the second Federation Instance, representing ENA.           |
| `openid.federation.registry.instances[1].name`                       | `ENA`                                  | Name of the second Federation Instance, representing ENA.                         |

---

Copyright &copy; 2025, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
