![Logo](../docs/images/sweden-connect.png)

# Organizations and Domains

This document describes how an organization bootstraps its record in the registry, how it claims the domains it
registers entities under, how the tenant operator reviews those claims, and what happens to registrations when a
claim is withdrawn or rejected.

## Table of Contents

- [Why domains](#why-domains)
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

## Why domains

Anyone holding write rights on an organization can submit a registration request for any entity identifier. Taken
alone, that lets organization A ask the federation to publish a subordinate statement for an entity living under
organization B's domain. Claimed domains close that gap: an organization states up front which hostnames it
controls, the tenant operator confirms the claim, and every later registration request is checked against the
claim rather than against the caller's rights alone.

Two things follow from that:

- A domain is **per organization, per tenant** — it hangs off the `organization` row, which is already scoped to
  one instance, and the claim row carries that instance too. Within one tenant a hostname has at most one holder
  (see [One holder per domain](#one-holder-per-domain)); across tenants the same hostname claimed by two
  organizations is two independent claims.
- A claim is evidence, not a grant. A `PENDING` claim already lets registrations through (see
  [enforcement](#domain-enforcement-on-registration-requests)); what the operator's review adds is the durable
  `VALIDATED` verdict, and the ability to say "no" and have the registrations that depended on it fall with it.

## Organization bootstrap

`Organization` (table `organization`) is created in one of two ways:

1. **Implicitly**, by `OrganizationService.findCreate`, the first time some other flow needs an organization row —
   creating an entity, running a registration flow. `legalName` stays `null` for these.
2. **Explicitly**, by the portal posting `POST /organization/v1/{tenant}/{orgNumber}` with a `legalName`. This is
   the path an onboarding portal drives.

`orgNumber` and the tenant come from the request path, `orgName` from the caller's `org_rights` token claim, and
`legalName` from the request body — the organization's own statement of its registered name, which the token claim
does not carry. The organization API never creates a record as a side effect of a read: `GET` on an organization
that has not bootstrapped is a **404**, which is what lets a portal distinguish "not onboarded" from "onboarded".

Both paths emit `ORGANIZATION_CREATED`.

## Domain lifecycle

A domain (`organization_domain`, entity `OrganizationDomain`) is a bare hostname — lower-cased, with no scheme, no
path, no port and no wildcard. An IP address is not a domain. A single-label name is rejected as well, except
`localhost`, which is accepted only where
`openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges` is `true` — the same
switch that lets the entity configuration loader reach a `https://localhost:6890/...` entity identifier at all.

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
      └──── DELETE /domains/{domainId} ────► (absent)     — allowed from any status
```

- **Request** (`POST .../domains`) creates the domain as `PENDING`. Claiming a domain the organization already
  claims is a **409**, *unless* the existing claim was `REJECTED`: that row is re-opened as `PENDING` with its
  review outcome cleared, and the call returns **201** with the same `domainId`. An organization can therefore
  correct and re-submit without the operator having to delete anything — provided the domain is still free, see
  [One holder per domain](#one-holder-per-domain).
- **Approve** (`POST /registration-admin/v1/.../domains/{domainId}/approve`) moves `PENDING → VALIDATED` and
  stamps `reviewedAt`/`reviewedBy`. Approving or rejecting a domain that is not `PENDING` is a **409** — a
  reviewed domain returns to `PENDING` only by being requested again.
- **Reject** (`POST /registration-admin/v1/.../domains/{domainId}/reject`) moves `PENDING → REJECTED`, stores the
  `rejectionReason`, and cascades (below).
- **Delete** (`DELETE .../domains/{domainId}`) removes the claim in any status. Deleting is the organization's own
  action and does **not** cascade — registrations already accepted under the domain are left alone. A domain ID
  belonging to another organization reads as a 404, the same as a nonexistent one.

## One holder per domain

**Only one organization on a tenant may hold a given domain at a time.** Holding means having a claim in status
`PENDING` or `VALIDATED`; a `REJECTED` claim, or a deleted one, releases the domain for somebody else to claim.

The rule is on the **exact domain string** only. There is no hierarchical check: while one organization holds
`example.se`, another organization may hold `a.example.se`, and neither claim blocks the other. Deciding who
really controls a name hierarchy is the operator's job at review time, not something the registry guesses at.

The scope is the **instance (tenant)**, not the registry as a whole. Two tenants are two federations; the same
organization number under tenant `ENA` may hold `example.se` while a different organization holds it under
tenant `Swedenconnect`.

Claiming a domain another organization on the tenant already holds is

```
409 Conflict
Domain <domain> is already held by another organization
```

(`ErrorTypes.CONFLICT`). The same answer comes back when the claim is a re-open of the organization's own
`REJECTED` row — a released claim is not a reservation, so re-requesting a domain somebody else has taken in the
meantime is refused exactly like a first claim would be. The operator gets the same 409 when approving a
`PENDING` claim on a domain another organization has come to hold since it was made.

Two organizations claiming the same free domain at the same instant both pass that check, so the database has
the final say: `organization_domain` carries an `instance_id` denormalized from the organization and a
persistent generated column

```sql
held_domain varchar(255) AS (CASE WHEN status IN ('PENDING','VALIDATED') THEN domain ELSE NULL END) PERSISTENT
```

under `UNIQUE KEY uk_instance_held_domain (instance_id, held_domain)`. A released claim stores `NULL` there, and
`NULL`s do not collide, so any number of `REJECTED` rows for the same domain coexist. The loser of the race is
handed the same 409 message as the pre-check would have given it, never a generic constraint error. The
per-organization `uk_organization_domain (organization_id, domain)` key is unchanged: an organization still has
at most one row per domain, whatever its status.

## Domain enforcement on registration requests

`RegistrationServiceImpl.createRegistrationRequest` and `updateRegistrationRequest` check the request before the
flow engine runs at all:

> The host of `entityIdentifier` must **equal**, or be a **subdomain of**, one of the calling organization's
> domains in status `PENDING` or `VALIDATED`.

"Subdomain of" means a label boundary, not a string suffix: `sp.example.com` matches `example.com`,
`notexample.com` does not. The rule lives in `organization/service/DomainMatcher`, which is pure and shared with
the cascade, so enforcement and cascade can never drift apart.

A request that fails the check is rejected with

```
400 Bad Request
Entity identifier host '<host>' is not a registered domain of organization <orgNumber>
```

as a problem detail (`ErrorTypes.INVALID_PARAMETER`). A **superuser is not exempt** — owning the registry does not
make somebody else's hostname yours to register.

## Cascading a domain rejection

Rejecting a domain the operator has decided the organization does not control has to reach the registrations that
were accepted on the strength of that claim. In the same transaction as the rejection, the registry rejects every
`Registration` that

- belongs to the domain's organization, and
- is `STARTED` or `PENDING_APPROVAL` (settled registrations are left alone), and
- is not a `TRUST_MARK_SUBORDINATE` (those follow their parent rather than being selected on their own), and
- has an `entityId` whose host matches the rejected domain **and matches no other `PENDING`/`VALIDATED` domain of
  the same organization.

That last clause is what keeps a registration alive when it is covered twice over: an organization holding both
`example.com` and `sp.example.com` does not lose `https://sp.example.com/oidf` when only one of the two is
rejected.

Each affected registration gets `status = REJECTED`, `rejectionReason = "Not Accepted Domain"`
(`RegistrationRejectionReasons.NOT_ACCEPTED_DOMAIN` — an exact string the portal matches on), plus `reviewedAt`
and `reviewedBy`. Its child `TRUST_MARK_SUBORDINATE` registrations that are not already `APPROVED` or `REJECTED`
are rejected with the same reason.

The `/reject` response is the updated `AdminDomainDto` fields plus `cascadedRegistrationIds`, which lists both the
parent registrations and the trust mark children that were rejected with them.

## Who reviews domains

**Decision (confirmed with the product owner):** the reviewer is the **tenant operator**. Concretely, a caller may
review domains when it is

- a **superuser**, or
- an organization with `canWrite` on `{orgNumber}` under `{tenant}` **that owns at least one
  `TrustAnchorIntermediateModule` of `ModuleType.TRUSTANCHOR` on that tenant's instance**.

Owning the tenant's trust anchor is the existing, durable marker of "this organization runs this federation", so
it is reused rather than adding a separate operator role. This is a deliberate choice, not a placeholder: if a
dedicated operator role is introduced later, `RegistrationAdminServiceImpl.requireReviewerInstance` is the single
place that changes.

Mechanically this is `@PreAuthorize("@orgRightsService.canWrite(authentication, #orgNumber, #tenant)")` on the
controller plus a service-level check. A caller that passes the rights check but owns no trust anchor gets a
**404**, following the same "foreign looks like missing" convention as `findOwnedRegistrationOrThrow` — an
endpoint you have no business calling should not confirm that it exists.

A reviewer sees the domains of **every organization on its own tenant's instance**, not just its own. Review is a
tenant-wide duty.

In the admin UI a domain request is reviewed from the **Registrations** view (`/registrations`), not a view of
its own: domain requests are listed alongside registration requests under the type **Domain**, next to `IM` and
`TM` in the type filter. A domain row shows the domain, the requesting organization, its status mapped onto the
registration status chips (`PENDING → Pending Approval`, `VALIDATED → Approved`, `REJECTED → Rejected`), when it
was requested and, once handled, the rejection reason. A **Show history** switch — off by default, persisted
under `oidf.registrations.showHistory` — decides whether handled requests are listed at all; with it off the
view is the unhandled queue only.

Clicking a domain row opens `/registrations/domain/{domainId}`, where the operator sees the full claim and
approves or rejects it. Rejection requires a reason and reports how many registrations the rejection cascaded
to (see [Cascading a domain rejection](#cascading-a-domain-rejection)).

## Pre-validated trust marks

`organization_trust_mark` holds trust mark types the tenant operator has pre-approved for an organization. They
are set with `PUT /registration-admin/v1/{tenant}/{orgNumber}/organizations/{targetOrgNumber}/trustmarks`, which
**replaces** the list wholesale, and are exposed on `OrganizationDto.preValidatedTrustMarks`.

Every entry is a trust mark type URI and is held to the same shape as an entity identifier — `https`, no query,
no fragment. A blank entry is a `400`, and so is the same type listed twice: the stored list is a set keyed on
`(organization, trustMarkType)`, so a duplicate is a mistake in the request rather than something to quietly
collapse. An empty list is valid and is how the operator withdraws every pre-approval.

In the admin UI this is the **Organizations** view (`/organizations`): one row per organization on the tenant,
with its domain counts and current pre-approvals, and an *Edit* dialog whose picker is filled from
`/trustmark-types` while still accepting a type typed in by hand. The domain count links to
`/registrations?type=DOMAIN&org=<orgNumber>&status=ALL&history=1` — that organization's domains, every status,
in the merged registrations view.

### Effect on trust mark enrollment

Pre-approving a type is a decision taken once, in advance, about an organization. Making the operator take it
again for every registration would be the same decision twice, so a registration that requests a type the
organization is already pre-approved for **does not stop for manual review**.

Nothing else changes. The trust mark sub-flow still runs — the same flow, the same steps, the same
`TRUST_MARK_SUBORDINATE` registration row and step trail. Only the manual-approval gate is passed instead of
halting:

1. `TrustMarkIssuerRegistrationStep` looks up, per requested type, whether the applicant organization holds that
   exact `trustMarkType` in `organization_trust_mark`. If it does, the step sets
   `ContextKey.TRUST_MARK_PRE_VALIDATED` on the **sub-flow's** context. The flag never reaches the parent
   context: pre-validation is per trust mark type, not a property of the registration as a whole.
2. `ProcessEngine`'s approval gate — the `manualreview=true` check — consults that flag. With it set, the engine
   does not return `pendingApproval`; it records the gated step with a `WARNING` result reading
   *"Auto-approved: organization holds a pre-validated trust mark for this enrollment"* and runs the step's
   `execute` as normal. The gate is engine-level, so it applies to whichever step in the sub-flow carries
   `manualreview=true`, not to one designated step.
3. Resuming an approved step (`approveStep` on a `TRUST_MARK_SUBORDINATE` registration) re-applies the flag when
   it rebuilds the context, so a *later* gated step in the same sub-flow auto-passes too, exactly as it would
   have on the first run.

An organization without the pre-approval runs the identical flow and waits for the operator, as before. The
parent step trail names which types were auto-approved (`Auto-approved (pre-validated): [...]`), and the child
registration's own trail carries the engine's note — so "this was never reviewed" is always visible after the
fact.

Withdrawing a type from the list stops future enrollments from skipping review; it does not revoke trust marks
already issued.

## API reference

### Portal API — `/organization/v1/{tenant}/{orgNumber}`

| Method   | Path                   | Right      | Behaviour                                                                                                                                                         |
|----------|------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`    | ``                     | `canRead`  | `200` `OrganizationDto`. `404` if the organization has not bootstrapped. Never auto-creates.                                                                      |
| `POST`   | ``                     | `canWrite` | Body `CreateOrganizationDto`. `201` `OrganizationDto`. `409` if it already exists, `400` on a blank legal name. Audit `ORGANIZATION_CREATED`.                     |
| `PUT`    | ``                     | `canWrite` | Body `CreateOrganizationDto`. Updates `legalName` only. `200`, `404` if missing. Audit `ORGANIZATION_UPDATED`.                                                    |
| `GET`    | `/domains`             | `canRead`  | `200` `List<DomainDto>`, every status. `404` if the organization is missing.                                                                                      |
| `POST`   | `/domains`             | `canWrite` | Body `DomainRequestDto`. `201` `DomainDto` (`PENDING`). `404` org missing, `400` invalid domain, `409` duplicate — except a `REJECTED` one, re-opened as `PENDING` — and `409` *`Domain <domain> is already held by another organization`* if another organization on the tenant holds it ([above](#one-holder-per-domain)). Audit `ORGANIZATION_DOMAIN_REQUESTED`. |
| `DELETE` | `/domains/{domainId}`  | `canWrite` | `204`. `404` if not owned by this organization. Any status may be deleted. Audit `ORGANIZATION_DOMAIN_DELETED`.                                                   |

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

### Operator API — `/registration-admin/v1/{tenant}/{orgNumber}`

Every endpoint below additionally requires the caller to be a tenant operator
([above](#who-reviews-domains)); a caller that is not gets a `404`.

| Method | Path                                              | Behaviour                                                                                                                       |
|--------|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/domains?status=PENDING`                         | `List<AdminDomainDto>` for every organization on the tenant's instance; `status` optional. `AdminDomainDto` = `DomainDto` + `orgNumber`, `orgName`, `legalName`. |
| `GET`  | `/domains/count`                                  | `{ "count": n }` — domains still `PENDING` on the tenant.                                                                       |
| `POST` | `/domains/{domainId}/approve`                     | `PENDING → VALIDATED`, sets `reviewedAt`/`reviewedBy`. `409` if not `PENDING`, and `409` *`Domain <domain> is already held by another organization`* if another organization on the tenant has come to hold it. Audit `ORGANIZATION_DOMAIN_APPROVED`. |
| `POST` | `/domains/{domainId}/reject`                      | Body `RejectRegistrationDto`. `PENDING → REJECTED`, then cascades. `409` if not `PENDING`. Audit `ORGANIZATION_DOMAIN_REJECTED`. |
| `GET`  | `/organizations`                                  | `List<AdminOrganizationDto>` — every organization on the tenant's instance, ordered by legal name, then organization name, then organization number. |
| `GET`  | `/organizations/{targetOrgNumber}`                | `OrganizationDto` for one organization on the tenant, domains included. `404` if it is not placed on this instance.              |
| `GET`  | `/trustmark-types`                                | `List<String>` — the distinct trust mark types issued on the tenant's instance, sorted. Populates the operator's trust mark picker. |
| `PUT`  | `/organizations/{targetOrgNumber}/trustmarks`     | Body `{ "preValidatedTrustMarks": [...] }` — replaces the list. `404` if the target organization is missing, `400` if an entry is blank, not an `https` URI, or listed twice. Audit `ORGANIZATION_UPDATED`. |

`AdminOrganizationDto` is the listing row, deliberately without the domains themselves — the operator fetches a
single organization to see those, so a tenant-wide listing stays one row per organization:

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

`/trustmark-types` answers "what can I pre-approve here": the types of every trust mark whose issuing entity
belongs to an organization on this instance. It is a suggestion list, not a constraint — the `PUT` accepts a type
that is not on it, because an organization may legitimately be pre-approved for a type its federation has not
issued yet.

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
| `openid.federation.registry.registration.require-registered-domain`  | No       | `true`  | Enforce the domain check on registration requests. Set to `false` only for an installation that manages entity ownership elsewhere. |

See [Application Configuration](configuration.md#registration).

## Audit events

| Event                          | Emitted when                                                                                        |
|--------------------------------|-------------------------------------------------------------------------------------------------------|
| `ORGANIZATION_CREATED`         | An organization record is created — by the portal, or implicitly by `OrganizationService.findCreate`. |
| `ORGANIZATION_UPDATED`         | An organization's legal name or pre-validated trust mark list changes.                               |
| `ORGANIZATION_DOMAIN_REQUESTED`| An organization claims a domain, or re-opens a rejected claim.                                       |
| `ORGANIZATION_DOMAIN_DELETED`  | An organization withdraws one of its domains.                                                        |
| `ORGANIZATION_DOMAIN_APPROVED` | The tenant operator approves a pending domain.                                                       |
| `ORGANIZATION_DOMAIN_REJECTED` | The tenant operator rejects a pending domain.                                                        |

See [Audit Events](audit.md) for the event structure.

## Contract deviations

The API contract is implemented as specified, with these clarifications where the specification left the detail
open:

- **`reviewedAt`/`reviewedBy` are cleared, not only `rejectionReason`, when a `REJECTED` domain is re-requested.**
  A re-opened domain reads as `PENDING`, and a pending domain carries no review outcome; leaving a stale
  `reviewedAt` behind would contradict `DomainDto`'s own documented shape. The JSON shape is unchanged.
- **`cascadedRegistrationIds` includes the rejected trust mark children**, not only their parent registrations.
  The field is a list of every registration the rejection changed, which is what a caller reporting "n
  registrations were rejected" needs.
- **`createdDate`/`reviewedAt` are serialized with a UTC offset** (`OffsetDateTime`, e.g.
  `2026-09-11T12:34:56.789+02:00`) rather than as a zone-less local timestamp. Both are ISO-8601 as the contract
  requires; the offset makes the value unambiguous for a consumer in another zone. Stored values remain
  `LocalDateTime`, resolved against the service's own zone at the mapper boundary.
- **`PUT /organizations/{targetOrgNumber}/trustmarks` returns `200` with the updated `OrganizationDto`.** The
  contract did not state a response body; returning the updated organization saves the caller a follow-up read.
- **The engine's auto-approval note does not name the trust mark type.** `ContextKey.TRUST_MARK_PRE_VALIDATED`
  carries `Boolean.TRUE`, so the gate itself has no type to quote; naming it there would have made the generic
  process engine read trust mark structures out of the context. The concrete types are named one level up
  instead, in `TrustMarkIssuerRegistrationStep`'s own step result
  (`Auto-approved (pre-validated): [<type>, ...]`), which is where an operator reading the parent registration
  sees them.

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
