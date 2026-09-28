![Logo](../docs/images/sweden-connect.png)

# Organizations and domains

This document covers how an organization creates its registry record, claims domains and has them reviewed. It
also covers what happens to registrations when a domain claim is rejected or deleted.

## Table of Contents

- [Purpose of domains](#purpose-of-domains)
- [Organization bootstrap](#organization-bootstrap)
- [Domain lifecycle](#domain-lifecycle)
- [One holder per domain](#one-holder-per-domain)
- [Domain enforcement on registration requests](#domain-enforcement-on-registration-requests)
- [Cascading a domain rejection](#cascading-a-domain-rejection)
- [Who reviews domains](#who-reviews-domains)
- [Pre-validated trust marks](#pre-validated-trust-marks)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Audit events](#audit-events)
- [Contract deviations](#contract-deviations)

---

## Purpose of domains

A registration request is accepted only for an entity identifier under a domain the organization has claimed.
Without this check, any organization with write rights could register an entity under another organization's
domain.

- A domain claim belongs to one organization on one tenant. The `organization_domain` row carries the instance
  of its organization. The same hostname on two tenants is two independent claims.
- A `PENDING` claim already lets registrations through (see
  [enforcement](#domain-enforcement-on-registration-requests)). Review sets the claim to `VALIDATED`, or rejects it
  together with the registrations that depend on it.

## Organization bootstrap

An `Organization` (table `organization`) is created in one of two ways:

1. **Implicitly**, by `OrganizationService.findCreate`, when another flow first needs the row, for example when
   creating an entity or running a registration flow. `legalName` stays `null`.
2. **Explicitly**, when the portal calls `POST /organization/v1/{tenant}/{orgNumber}` with a `legalName`.

The values come from these sources:

| Field        | Source                                  |
|--------------|-----------------------------------------|
| `orgNumber`  | Request path                            |
| tenant       | Request path                            |
| `orgName`    | The caller's `org_rights` token claim   |
| `legalName`  | Request body                            |

A read never creates a record. `GET` on an organization that is not bootstrapped returns `404`, so a portal can
tell whether the organization is onboarded.

Both paths emit `ORGANIZATION_CREATED`.

## Domain lifecycle

A domain (`organization_domain`, entity `OrganizationDomain`) is a bare hostname in lower case. It has no scheme,
path, port or wildcard. IP addresses and single-label names are rejected. The exception is `localhost`, which is
accepted when `openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges` is `true`.

```
                    POST /domains
   (absent) ─────────────────────────────► PENDING
                                            │   │
                 approve                    │   │                  reject
      ┌─────────────────────────────────────┘   └──────────────────────────────────┐
      ▼                                                                            ▼
  VALIDATED                                                                    REJECTED
      │                                                                            │
      │                                       POST /domains (same domain)          │
      │                                  ┌─────────────────────────────────────────┘
      │                                  ▼
      │                               PENDING  (same row re-opened; rejectionReason,
      │                                         reviewedAt and reviewedBy cleared)
      │
      └──── DELETE /domains/{domainId} ────► (absent)     (allowed from any status)
```

The transitions work as follows:

- **Request** (`POST .../domains`) creates the domain as `PENDING`. A domain the organization already claims
  returns `409`. The exception is a `REJECTED` claim: the row is re-opened as `PENDING`, its review outcome is
  cleared, and the call returns `201` with the same `domainId`. The domain must still be free (see
  [One holder per domain](#one-holder-per-domain)).
- **Approve** (`POST /registration-admin/v1/.../domains/{domainId}/approve`) moves `PENDING` to `VALIDATED` and
  sets `reviewedAt` and `reviewedBy`.
- **Reject** (`POST /registration-admin/v1/.../domains/{domainId}/reject`) moves `PENDING` to `REJECTED`, stores
  the `rejectionReason` and cascades (see [Cascading a domain rejection](#cascading-a-domain-rejection)).
- **Delete** (`DELETE .../domains/{domainId}`) removes the claim in any status. Deletion does not cascade:
  registrations accepted under the domain stay. A domain ID of another organization returns `404`.

Approving or rejecting a domain that is not `PENDING` returns `409`. A reviewed domain returns to `PENDING` only
when it is requested again.

## One holder per domain

Only one organization on a tenant can hold a given domain. A claim in status `PENDING` or `VALIDATED` holds the
domain. A `REJECTED` or deleted claim releases it.

The rule compares the exact domain string. While one organization holds `example.se`, another can hold
`a.example.se`. The operator decides who controls a name hierarchy at review time.

The rule applies per instance (tenant). Under tenant `ENA` one organization can hold `example.se` while a
different organization holds it under tenant `Swedenconnect`.

Claiming a domain that another organization on the tenant holds returns:

```
409 Conflict
Domain <domain> is already held by another organization
```

(`ErrorTypes.CONFLICT`). The same response applies in two more cases:

- Re-opening an own `REJECTED` claim after another organization has claimed the domain.
- Approving a `PENDING` claim after another organization has come to hold the domain.

The database enforces the rule for concurrent claims. `organization_domain` carries an `instance_id` copied from
the organization and a persistent generated column:

```sql
held_domain varchar(255) AS (CASE WHEN status IN ('PENDING','VALIDATED') THEN domain ELSE NULL END) PERSISTENT
```

`UNIQUE KEY uk_instance_held_domain (instance_id, held_domain)` covers it. A released claim stores `NULL`, so any
number of `REJECTED` rows for the same domain can exist. The losing request gets the same `409` message, not a
constraint error. `uk_organization_domain (organization_id, domain)` still limits an organization to one row per
domain.

## Domain enforcement on registration requests

`RegistrationServiceImpl.createRegistrationRequest` and `updateRegistrationRequest` check each request before the
flow engine runs:

> The host of `entityIdentifier` must **equal**, or be a **subdomain of**, one of the calling organization's
> domains in status `PENDING` or `VALIDATED`.

A subdomain match follows label boundaries: `sp.example.com` matches `example.com`, `notexample.com` does not.
`organization/service/DomainMatcher` holds the rule and is shared with the cascade.

A request that fails the check returns a problem detail (`ErrorTypes.INVALID_PARAMETER`):

```
400 Bad Request
Entity identifier host '<host>' is not a registered domain of organization <orgNumber>
```

The check also applies to superusers.

## Cascading a domain rejection

A domain rejection also rejects the registrations accepted under that domain, in the same transaction. A
`Registration` is rejected when it:

- belongs to the domain's organization
- is `STARTED` or `PENDING_APPROVAL`
- is not a `TRUST_MARK_SUBORDINATE` (these follow their parent)
- has an `entityId` host that matches the rejected domain **and no other `PENDING`/`VALIDATED` domain of the same
  organization**

The last condition keeps registrations covered by another domain. If an organization holds `example.com` and
`sp.example.com`, rejecting one of them does not reject `https://sp.example.com/oidf`.

Each affected registration gets `status = REJECTED`, `reviewedAt`, `reviewedBy` and
`rejectionReason = "Not Accepted Domain"` (`RegistrationRejectionReasons.NOT_ACCEPTED_DOMAIN`). The portal
matches on this exact string. Child `TRUST_MARK_SUBORDINATE` registrations that are not `APPROVED` or `REJECTED`
get the same reason.

The `/reject` response contains the `AdminDomainDto` fields and `cascadedRegistrationIds`. The list holds both
the parent registrations and their rejected trust mark children.

## Who reviews domains

The tenant operator reviews domains. A caller can review when it is:

- a **superuser**, or
- an organization with `canWrite` on `{orgNumber}` under `{tenant}` that is listed in the tenant's
  `openid.federation.registry.instances[i].operator_organizations` (see
  [Application Configuration](configuration.md#instance-properties)).

Owning a trust anchor does not make an organization an operator, because any organization with `write` can create
one. A tenant without `operator_organizations` has only superusers as operators.
`RegistrationAdminServiceImpl.requireReviewerInstance` enforces the rule.

The controller carries `@PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")`, and
the service checks the operator. A caller that passes the rights check but is not an operator gets `404`, the
same as `findOwnedRegistrationOrThrow` returns for a foreign registration.

A reviewer sees the domains of every organization on its tenant.

### Review in the admin UI

Domain requests are reviewed in the **Registrations** view (`/registrations`), listed with registration requests
under the type **Domain**. A domain row shows:

- the domain and the requesting organization
- the status, mapped onto the registration chips: `PENDING` to Pending Approval, `VALIDATED` to Approved,
  `REJECTED` to Rejected
- the request date
- the rejection reason, when there is one

The **Show history** switch is disabled by default and stored under `oidf.registrations.showHistory`. When
disabled, the view lists only unhandled requests.

Selecting a domain row opens `/registrations/domain/{domainId}`, where the operator approves or rejects the
claim. A rejection requires a reason. The result shows how many registrations the rejection cascaded to.

## Pre-validated trust marks

`organization_trust_mark` holds the trust mark types the tenant operator has pre-approved for an organization.
`PUT /registration-admin/v1/{tenant}/{orgNumber}/organizations/{targetOrgNumber}/trustmarks` replaces the whole
list. `OrganizationDto.preValidatedTrustMarks` exposes it.

Each entry is a trust mark type URI with the same shape rules as an entity identifier: `https`, no query, no
fragment. A blank or duplicate entry returns `400`. An empty list is valid and removes every pre-approval.

### Operator views in the admin UI

The **Organizations** view (`/organizations`) has one row per organization on the tenant. A row shows its domain
counts and pre-approvals, and has an *Edit* dialog. The dialog's picker lists the types from `/trustmark-types`
and also accepts a typed value. The domain count links to
`/registrations?type=DOMAIN&org=<orgNumber>&status=ALL&history=1`.

The admin UI shows operator features only when the *selected* organization is an operator of the selected
tenant. The `operator` flag on the organization's `/tenants` entry controls this. Operator features are:

- the **Organizations**, **Registrations** and **Registration Flows** nav tabs
- *Add Federation Entity*, and the Edit and Delete actions on federation entities
- the trust mark's flow assignment

A user with another organization selected sees none of these, superusers included. Routes marked `operatorOnly`
redirect to the Entity view. The backend enforces the same rule (see
[Authorization Model](oauth.md#tenant-operator)).

### Effect on trust mark enrollment

A registration that requests a pre-approved trust mark type skips manual review. The trust mark sub-flow still
runs with the same flow, steps, `TRUST_MARK_SUBORDINATE` row and step trail. Only the approval gate changes:

1. `TrustMarkIssuerRegistrationStep` checks, per requested type, whether `organization_trust_mark` holds that
   exact `trustMarkType` for the organization. If it does, the step sets `ContextKey.TRUST_MARK_PRE_VALIDATED` on
   the **sub-flow** context. The parent context never gets the flag.
2. The approval gate in `ProcessEngine` (the `manualreview=true` check) reads the flag. When set, the engine
   runs the step's `execute` and records a `WARNING` result: *"Auto-approved: organization holds a pre-validated
   trust mark for this enrollment"*. This applies to any step in the sub-flow with `manualreview=true`.
3. `approveStep` on a `TRUST_MARK_SUBORDINATE` registration sets the flag again when it rebuilds the context.
   A later gated step in the same sub-flow then passes as well.

An organization without the pre-approval waits for the operator. The parent step trail lists the auto-approved
types (`Auto-approved (pre-validated): [...]`), and the child registration's trail carries the engine's note.

Removing a type from the list affects future enrollments only. Trust marks already issued stay.

## API reference

### Portal API: `/organization/v1/{tenant}/{orgNumber}`

| Method   | Path                   | Right      | Behaviour                                                                                                                                                         |
|----------|------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`    | ``                     | `canRead`  | `200` `OrganizationDto`. `404` if the organization is not bootstrapped. Never creates a record.                                                                   |
| `POST`   | ``                     | `canWrite` | Body `CreateOrganizationDto`. `201` `OrganizationDto`. `409` if it exists, `400` on a blank legal name. Audit `ORGANIZATION_CREATED`.                             |
| `PUT`    | ``                     | `canWrite` | Body `CreateOrganizationDto`. Updates `legalName` only. `200`, or `404` if missing. Audit `ORGANIZATION_UPDATED`.                                                 |
| `GET`    | `/domains`             | `canRead`  | `200` `List<DomainDto>`, every status. `404` if the organization is missing.                                                                                      |
| `POST`   | `/domains`             | `canWrite` | Body `DomainRequestDto`. `201` `DomainDto` (`PENDING`). `404` if the organization is missing, `400` on an invalid domain, `409` on a duplicate (a `REJECTED` claim is re-opened as `PENDING` instead). `409` *`Domain <domain> is already held by another organization`* if another organization on the tenant holds it ([one holder per domain](#one-holder-per-domain)). Audit `ORGANIZATION_DOMAIN_REQUESTED`. |
| `DELETE` | `/domains/{domainId}`  | `canWrite` | `204`. `404` if another organization owns it. Any status can be deleted. Audit `ORGANIZATION_DOMAIN_DELETED`.                                                     |

```json
OrganizationDto {
  "orgNumber": "5520012229",
  "orgName": "TestOrg1",
  "legalName": "TestOrg1 AB",
  "tenant": "swedenconnect",
  "domains": [ DomainDto ],
  "preValidatedTrustMarks": [ "https://tm.example.com/tm/x" ]
}
DomainDto {
  "domainId": "uuid",
  "domain": "example.com",
  "status": "PENDING" | "VALIDATED" | "REJECTED",
  "rejectionReason": null | "...",
  "createdDate": "ISO-8601",
  "reviewedAt": null | "ISO-8601"
}
```

### Operator API: `/registration-admin/v1/{tenant}/{orgNumber}`

Each endpoint below requires the caller to be a tenant operator ([who reviews domains](#who-reviews-domains)).
Other callers get `404`.

| Method | Path                                              | Behaviour                                                                                                                       |
|--------|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/domains?status=PENDING`                         | `List<AdminDomainDto>` for every organization on the tenant. `status` is optional. `AdminDomainDto` is `DomainDto` plus `orgNumber`, `orgName` and `legalName`. |
| `GET`  | `/domains/count`                                  | `{ "count": n }`: the number of `PENDING` domains on the tenant.                                                                |
| `POST` | `/domains/{domainId}/approve`                     | `PENDING` to `VALIDATED`, sets `reviewedAt` and `reviewedBy`. `409` if not `PENDING`. `409` *`Domain <domain> is already held by another organization`* if another organization on the tenant holds it. Audit `ORGANIZATION_DOMAIN_APPROVED`. |
| `POST` | `/domains/{domainId}/reject`                      | Body `RejectRegistrationDto`. `PENDING` to `REJECTED`, then cascades. `409` if not `PENDING`. Audit `ORGANIZATION_DOMAIN_REJECTED`. |
| `GET`  | `/organizations`                                  | `List<AdminOrganizationDto>` for every organization on the tenant, ordered by legal name, organization name and organization number. |
| `GET`  | `/organizations/{targetOrgNumber}`                | `OrganizationDto` for one organization on the tenant, with domains. `404` if it is not on this instance.                         |
| `GET`  | `/trustmark-types`                                | `List<String>`: the distinct trust mark types issued on the tenant, sorted. Fills the operator's trust mark picker.             |
| `PUT`  | `/organizations/{targetOrgNumber}/trustmarks`     | Body `{ "preValidatedTrustMarks": [...] }`. Replaces the list. `404` if the target organization is missing, `400` if an entry is blank, not an `https` URI, or duplicated. Audit `ORGANIZATION_UPDATED`. |

`AdminOrganizationDto` is the listing row. It has domain counts but no domains; fetch a single organization to
see them:

```json
{
  "orgNumber": "5520012229",
  "orgName": "TestOrg1",
  "legalName": "TestOrg1 AB",
  "domainCount": 3,
  "pendingDomainCount": 1,
  "preValidatedTrustMarks": [ "https://tm.example.com/tm/x" ]
}
```

`/trustmark-types` returns the types of every trust mark whose issuing entity belongs to an organization on this
instance. The list is a suggestion. The `PUT` also accepts other types, because an organization can be
pre-approved for a type the federation has not issued yet.

The `/reject` response body:

```json
{
  "domainId": "uuid",
  "domain": "example.com",
  "status": "REJECTED",
  "rejectionReason": "...",
  "createdDate": "ISO-8601",
  "reviewedAt": "ISO-8601",
  "orgNumber": "5520012229",
  "orgName": "TestOrg1",
  "legalName": "TestOrg1 AB",
  "cascadedRegistrationIds": [ "uuid" ]
}
```

## Configuration

| Setting                                                              | Required | Default | Description                                                                                                              |
|----------------------------------------------------------------------|----------|---------|----------------------------------------------------------------------------------------------------------------------------|
| `openid.federation.registry.registration.require-registered-domain`  | No       | `true`  | Enforces the domain check on registration requests. Set to `false` only where entity ownership is managed outside the registry. |

See [Application Configuration](configuration.md#registration).

## Audit events

| Event                          | Emitted when                                                                                        |
|--------------------------------|-------------------------------------------------------------------------------------------------------|
| `ORGANIZATION_CREATED`         | An organization record is created, by the portal or by `OrganizationService.findCreate`.            |
| `ORGANIZATION_UPDATED`         | An organization's legal name or pre-validated trust mark list changes.                               |
| `ORGANIZATION_DOMAIN_REQUESTED`| An organization claims a domain or re-opens a rejected claim.                                         |
| `ORGANIZATION_DOMAIN_DELETED`  | An organization deletes one of its domains.                                                          |
| `ORGANIZATION_DOMAIN_APPROVED` | The tenant operator approves a pending domain.                                                       |
| `ORGANIZATION_DOMAIN_REJECTED` | The tenant operator rejects a pending domain.                                                        |

See [Audit Events](audit.md) for the event structure.

## Contract deviations

The implementation follows the API contract. Where the contract left details open, the implementation does the
following:

- **Re-requesting a `REJECTED` domain clears `reviewedAt` and `reviewedBy` as well as `rejectionReason`.** A
  `PENDING` domain carries no review outcome. The JSON shape is unchanged.
- **`cascadedRegistrationIds` includes the rejected trust mark children** as well as their parents. The list
  holds every registration the rejection changed.
- **`createdDate` and `reviewedAt` carry a UTC offset** (`OffsetDateTime`, for example
  `2026-09-11T12:34:56.789+02:00`). Both are ISO-8601 as the contract requires. The database stores
  `LocalDateTime`, and the mapper applies the service's time zone.
- **`PUT /organizations/{targetOrgNumber}/trustmarks` returns `200` with the updated `OrganizationDto`.** The
  contract did not specify a response body.
- **The engine's auto-approval note does not name the trust mark type.** `ContextKey.TRUST_MARK_PRE_VALIDATED`
  holds `Boolean.TRUE` only. `TrustMarkIssuerRegistrationStep` names the types in its own step result
  (`Auto-approved (pre-validated): [<type>, ...]`), which the operator sees on the parent registration.

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
