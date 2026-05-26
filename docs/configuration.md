![Logo](../docs/images/sweden-connect.png)

# Application Configuration

This document describes the configuration settings available for the OpenID Federation Entity Registry Service.

## Table of Contents

- [Server Configuration](#server-configuration)
- [Spring Application Configuration](#spring-application-configuration)
- [Database Configuration](#database-configuration)
- [Security Configuration](#security-configuration)
- [Logging Configuration](#logging-configuration)
- [Observability](#observability)
- [API Documentation (SpringDoc)](#api-documentation-springdoc)
- [Credential Bundles](#credential-bundles)
- [Audit Logging](#audit-logging)
- [OpenID Federation Registry Settings](#openid-federation-registry-settings)
  - [Federation Service API](#federation-service-api)
  - [Federation Instances](#federation-instances)
  - [Entity Configuration Loader](#entity-configuration-loader)

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

## Observability

| Setting                                                 | Example Value                     | Description                                                                                    |
|---------------------------------------------------------|-----------------------------------|------------------------------------------------------------------------------------------------|
| `management.server.port`                                | `8081`                            | Port for the management/actuator endpoints (separate from the main server port).               |
| `management.metrics.tags.application_name`              | `oidf-registry`                   | Tag added to all metrics for filtering by application name.                                    |
| `management.metrics.tags.application_version`           | `1.0.0`                           | Tag added to all metrics for filtering by application version.                                 |
| `management.prometheus.metrics.export.enabled`          | `true`                            | Enables Prometheus metrics export.                                                             |
| `management.tracing.sampling.probability`               | `1.0`                             | Fraction of requests to sample for distributed tracing. `1.0` = 100 %.                         |
| `management.opentelemetry.tracing.export.otlp.endpoint` | `http://localhost:4318/v1/traces` | OTLP endpoint for exporting traces. Can also be set via `OTEL_EXPORTER_OTLP_ENDPOINT` env var. |
| `management.endpoints.web.exposure.include`             | `*`                               | Controls which actuator endpoints are exposed over HTTP.                                       |

## API Documentation (SpringDoc)

| Setting                                     | Example Value      | Description                                                      |
|---------------------------------------------|--------------------|------------------------------------------------------------------|
| `springdoc.api-docs.path`                   | `/v3/api-docs`     | Path where the OpenAPI JSON descriptor is served.                |
| `springdoc.api-docs.enabled`                | `true`             | Enables or disables the OpenAPI descriptor endpoint.             |
| `springdoc.swagger-ui.path`                 | `/swagger-ui.html` | Path where the Swagger UI is served.                             |
| `springdoc.swagger-ui.enabled`              | `true`             | Enables or disables the Swagger UI.                              |
| `springdoc.swagger-ui.operationsSorter`     | `method`           | Sort order for operations in the UI (`method` or `alpha`).       |
| `springdoc.swagger-ui.tagsSorter`           | `alpha`            | Sort order for tags in the UI.                                   |
| `springdoc.swagger-ui.tryItOutEnabled`      | `true`             | Enables the "Try it out" button in Swagger UI by default.        |
| `springdoc.swagger-ui.filter`               | `true`             | Enables the filter/search box in Swagger UI.                     |
| `springdoc.swagger-ui.persistAuthorization` | `true`             | Persists authorization tokens across page reloads in Swagger UI. |

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

Each entry in `openid.federation.registry.instances` represents one federation instance managed by
this registry. An instance maps a set of organisations to a specific federation endpoint.

#### Instance properties

| Setting                                                          | Required | Example Value                            | Description                                                                                                                                                    |
|------------------------------------------------------------------|----------|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `openid.federation.registry.instances[i].instance_id`            | Yes      | `123e4567-e89b-12d3-a456-426614174000`   | UUID that uniquely identifies this instance. Must match the instance record in the database.                                                                   |
| `openid.federation.registry.instances[i].name`                   | Yes      | `Swedenconnect`                          | Human-readable name for the instance.                                                                                                                          |
| `openid.federation.registry.instances[i].base_url`               | Yes      | `https://registry.swedenconnect.se/oidf` | Base URL for this instance. Used to compute the `entityPrefix` for every organisation assigned to it: `base_url/orgNumber`.                                    |
| `openid.federation.registry.instances[i].org_base_url_overrides` | No       | See example below                        | Optional per-organisation override of `base_url`. When set for an org, its `entityPrefix` is computed as `override/orgNumber` instead of `base_url/orgNumber`. |
| `openid.federation.registry.instances[i].matchers`               | Yes      | See below                                | Matcher configuration that determines which organisations are assigned to this instance.                                                                       |

#### Entity prefix computation

The `entityPrefix` for an organisation is resolved at request time — it is not stored in the token:

1. The matching instance is found using `matchers` (see below).
2. If the organisation number exists in `org_base_url_overrides`, the override URL is used as the base.
3. Otherwise `base_url` is used.
4. The final value is `<base>/orgNumber`, e.g. `https://registry.swedenconnect.se/oidf/5590026042`.

#### Per-organisation URL override

```yaml
openid:
  federation:
    registry:
      instances:
        - instance_id: "123e4567-e89b-12d3-a456-426614174000"
          name: "Swedenconnect"
          base_url: "https://registry.swedenconnect.se/oidf"
          org_base_url_overrides:
            "5590026042": "https://dev.swedenconnect.se/oidf-test"
          matchers:
            useForDefaultAssignment: true
```

In this example all organisations use `https://registry.swedenconnect.se/oidf/{orgNumber}` except
`5590026042`, which resolves to `https://dev.swedenconnect.se/oidf-test/5590026042`.

#### Matchers

Matchers control which organisations are routed to a given instance.
Exactly one of the three properties must be active per instance.

| Setting                                                                    | Example Value       | Description                                                                                                                                                                                         |
|----------------------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `openid.federation.registry.instances[i].matchers.org_numbers`             | `["5590026042"]`    | Explicit list of organisation numbers assigned to this instance. Takes precedence over `functiongroups`.                                                                                            |
| `openid.federation.registry.instances[i].matchers.functiongroups`          | `["swedenconnect"]` | List of function group identifiers. Matches after `org_numbers`. Cannot be combined with `useForDefaultAssignment`.                                                                                 |
| `openid.federation.registry.instances[i].matchers.useForDefaultAssignment` | `true`              | When `true`, this instance receives all organisations that do not match any other instance. At most one instance may set this to `true`. Cannot be combined with `org_numbers` or `functiongroups`. |

**Matching order** (evaluated per request, first match wins):

1. `org_numbers` — exact match on organisation number.
2. `functiongroups` — match on the organisation's function group.
3. `useForDefaultAssignment` — catch-all fallback.

#### Full example

```yaml
openid:
  federation:
    registry:
      instances:
        - instance_id: "123e4567-e89b-12d3-a456-426614174000"
          name: "Swedenconnect"
          base_url: "https://registry.swedenconnect.se/oidf"
          matchers:
            useForDefaultAssignment: true

        - instance_id: "223e4567-e89b-12d3-a456-426614174001"
          name: "ENA"
          base_url: "https://registry.ena.se/oidf"
          matchers:
            org_numbers:
              - "2021002114"
              - "5590026042"
```

### Entity Configuration Loader

Controls how the registry fetches and validates entity configurations from external entities during
registration flows. All settings are optional; the loader is disabled by default.

| Setting                                                                                 | Required | Default | Description                                                                                                                             |
|-----------------------------------------------------------------------------------------|----------|---------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `openid.federation.registry.entity-configuration-loader.enabled`                        | No       | `false` | Enables loading entity configuration (and JWKS) from an entity's self-signed entity statement during registration.                      |
| `openid.federation.registry.entity-configuration-loader.trust-bundle-alias`             | No       | —       | Alias of the Spring credential bundle used to trust HTTPS connections when fetching remote entity configurations.                       |
| `openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges` | No       | `false` | When `true`, the loader may resolve entity IDs that map to private/local IP ranges. **Security risk** — enables SSRF to internal hosts. |
| `openid.federation.registry.entity-configuration-loader.disable-system-properties`      | No       | `false` | When `true`, JVM system properties (e.g. proxy settings) are ignored when building the outgoing HTTP client.                            |
| `openid.federation.registry.entity-configuration-loader.block-hostname`                 | No       | —       | List of regular expressions. Any entity ID whose host matches one of these patterns is rejected before the outgoing request is made.    |

#### Example

```yaml
openid:
  federation:
    registry:
      entity_configuration_loader:
        enabled: true
        trust-bundle-alias: "myTrustBundle"
        enable-local-ip-address-ranges: false
        block-hostname:
          - ".*\\.internal\\.example\\.com"
          - "localhost"
```

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
