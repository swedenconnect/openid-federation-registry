![Logo](../docs/images/sweden-connect.png)

# Audit Event Documentation

This document details the audit events emitted by the registry service (`RegistryAuditEventType`,
`RegistryAuditServiceAdapter`), what triggers them, their structure, and how to read them.

## Table of Contents

- [Event Structure](#event-structure)
- [Event Types](#event-types)
- [Configuring Audit Output](#configuring-audit-output)

---

## Event Structure

Every audit event is emitted as a Spring Boot Actuator `AuditEvent` — `principal`, event `type` (the
`RegistryAuditEventType` name), and a `data` map built from `FederationAuditEvent`. The same fixed set of fields
is used across all event types; which ones are present depends on the event:

| Field            | Present on                                      | Description                                                                                                                                  |
|------------------|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `extId`          | All events                                      | External identifier of the affected resource — a UUID for entity/module/trustmark/subordinate events, a URI for the two system-level events. |
| `organizationId` | All resource CRUD events                        | UUID of the organization the resource belongs to.                                                                                            |
| `instanceId`     | All resource CRUD events                        | UUID of the instance (tenant) the resource belongs to.                                                                                       |
| `oldData`        | Update and delete events                        | JSON representation of the resource's prior state.                                                                                           |
| `newData`        | Create and update events, `LOADED_SERVICE_KEYS` | JSON representation of the resource's new state (or, for `LOADED_SERVICE_KEYS`, the list of loaded key IDs).                                 |

`oldData` is omitted when it's identical to `newData`. The two system-level events
(`RESOLVED_ENTITY_CONFIGURATION`, `LOADED_SERVICE_KEYS`) carry only `extId` (and `newData` for the latter) — they
are not scoped to an organization or instance.

## Event Types

`RegistryAuditEventType` groups into resource CRUD events (one `_CREATED`/`_UPDATED`/`_DELETED` triple per
resource kind) plus two system-level events.

| Resource kind       | Event names                                                                                                                                                                                                                                       |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Federation entity   | `FEDERATION_ENTITY_CREATED`, `FEDERATION_ENTITY_UPDATED`, `FEDERATION_ENTITY_DELETED`                                                                                                                                                             |
| Hosted entity       | `HOSTED_ENTITY_CREATED`, `HOSTED_ENTITY_UPDATED`, `HOSTED_ENTITY_DELETED`                                                                                                                                                                         |
| Subordinate entity  | `SUBORDINATE_ENTITY_CREATED`, `SUBORDINATE_ENTITY_UPDATED`, `SUBORDINATE_ENTITY_DELETED` — reserved for future use; not currently emitted (see `SUBORDINATE_*` below for the events actually emitted by `subordinateCreated`/`Updated`/`Deleted`) |
| Trust anchor module | `TRUST_ANCHOR_CREATED`, `TRUST_ANCHOR_UPDATED`, `TRUST_ANCHOR_DELETED`                                                                                                                                                                            |
| Intermediate module | `INTERMEDIATE_CREATED`, `INTERMEDIATE_UPDATED`, `INTERMEDIATE_DELETED`                                                                                                                                                                            |
| Resolver module     | `RESOLVER_CREATED`, `RESOLVER_UPDATED`, `RESOLVER_DELETED`                                                                                                                                                                                        |
| Trustmark           | `TRUSTMARK_CREATED`, `TRUSTMARK_UPDATED`, `TRUSTMARK_DELETED`                                                                                                                                                                                     |
| Trustmark subject   | `TRUSTMARK_SUBJECT_CREATED`, `TRUSTMARK_SUBJECT_UPDATED`, `TRUSTMARK_SUBJECT_DELETED`                                                                                                                                                             |
| Trustmark issuer    | `TRUSTMARK_ISSUER_CREATED`, `TRUSTMARK_ISSUER_UPDATED`, `TRUSTMARK_ISSUER_DELETED`                                                                                                                                                                |
| Subordinate         | `SUBORDINATE_CREATED`, `SUBORDINATE_UPDATED`, `SUBORDINATE_DELETED`                                                                                                                                                                               |

System-level (no organization/instance scope):

| Event                           | Emitted when                                                                                                                       |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `RESOLVED_ENTITY_CONFIGURATION` | The entity configuration loader fetches and resolves an entity's own `.well-known` configuration. `extId` is the location fetched. |
| `LOADED_SERVICE_KEYS`           | JWKS keys are loaded from an oidf-service node. `extId` is the JWKS URI; `newData` is the list of loaded key IDs.                  |

> `SUBORDINATE_ENTITY_*` is defined in the enum but has no emitting call site in `RegistryAuditServiceAdapter` at
> the time of writing — subordinate lifecycle events are emitted as plain `SUBORDINATE_*` instead. Treat the enum
> (`infrastructure.audit.RegistryAuditEventType`) as the source of truth if this document and the code diverge
> again.

## Configuring Audit Output

Audit events are published as standard Spring Boot Actuator `AuditEvent`s and can be consumed through any
`AuditEventRepository`/listener configured for the application; see
[Application Configuration](configuration.md#audit-logging) for the relevant properties.

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
