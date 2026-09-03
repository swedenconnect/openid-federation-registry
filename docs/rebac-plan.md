![Logo](../docs/images/sweden-connect.png)

# ReBAC Access Management — Implementation Plan

This document describes a plan for remodelling resource access management in the Entity Registry as
relationship-based access control (ReBAC), scoped to **organization-level** granularity and implemented
**in-process** rather than against a separate OpenFGA deployment.

## Table of Contents

- [Summary](#summary)
- [Current State](#current-state)
- [Scope Decisions](#scope-decisions)
- [Design](#design)
- [Work Breakdown](#work-breakdown)
- [Trade-offs](#trade-offs)
- [Migration Path to OpenFGA](#migration-path-to-openfga)

## Summary

| Aspect | Outcome |
|--------|---------|
| Type of change | Cross-cutting refactor. No rewrite. |
| Domain model / DB schema | Unchanged, plus one new table |
| Granularity | Organization-level only. No per-user relations. |
| Runtime dependency | None. No OpenFGA server. |
| Estimated effort | 4–6 days, one developer |

The ownership hierarchy that ReBAC needs already exists as JPA relations. The plan below stores only the
relationships the foreign-key graph cannot express, and derives the rest.

## Current State

Two independent mechanisms exist today. Neither is ReBAC.

### 1. Coarse scope checks

`infrastructure/config/SecurityConfig.java` declares roughly 20 URL + HTTP-method rules of the form:

```java
.requestMatchers(HttpMethod.POST, "/registry/v1/subordinates/**")
.hasAuthority("SCOPE_http://registry.swedenconnect.se/subordinates/write")
```

These are global, not per-resource. There is no role concept. See [OAuth Scopes](oauth.md).

### 2. Tenancy by data filtering

`OrganizationRecord` is resolved from the JWT `org` claim (or the HTTP session for GUI logins) by
`infrastructure/auth/OrganizationRecordClaimSelector.java`, injected as a hidden controller argument at
roughly 60 call sites, threaded through the service layer, and ends up as a query predicate:

```java
@Query("SELECT r FROM Resolver r JOIN r.entity e JOIN e.organization o "
    + "WHERE o.orgNumber = :orgNumber")
List<Resolver> findByOrgNumber(@Param("orgNumber") String orgNumber);
```

22 repository methods take `orgNumber`. **Authorization is currently the query.** That is the thing this
plan moves behind an explicit seam.

### Ownership hierarchy

Already complete in JPA, acyclic, maximum depth 3:

```
Instance
  └── Organization
        ├── FederationEntity
        │     ├── Resolver
        │     └── TrustMarkIssuer
        │           └── TrustMark
        │                 └── TrustMarkSubject
        ├── TrustAnchorIntermediateModule (TaIm)
        │     ├── Subordinate
        │     └── FlowAssignment
        │           └── Registration
        └── RegistrationFlow
```

### Known defect this plan fixes

`registrations/service/RegistrationAdminServiceImpl.java:96` carries the comment
`//TODO this is not the right way to handle organizations`. It calls `findAll()` and filters in memory,
because a `Registration` is associated with **two** organizations — the IM/TMI owner who approves it, and
the applicant being registered — and a single `orgNumber` column cannot express that.

## Scope Decisions

### Organization-level only

Access is granted to organizations, never to individual users. The check subject is
`organization:<orgNumber>`, taken directly from the resolved `OrganizationRecord`.

Consequence: user identity never enters the access model. No membership tuples, no user provisioning, no
IdP claim synchronisation. Read/write distinction stays where it is today — OAuth scopes in
`SecurityConfig`.

The resulting split is clean:

> **Scopes decide what a user may do. ReBAC decides which org owns what.**

### No separate OpenFGA service

At organization granularity, an OpenFGA deployment would exercise roughly 5% of its feature set. Userset
rewrite evaluation, the Leopard index, zookie consistency, arbitrary-depth traversal, contextual tuples
and per-user fan-out all go unused against a fixed-shape, depth-3, acyclic graph.

The decisive factor is not the container count — it is transactional integrity. In-process, relationship
rows live in the same MariaDB and are written in the same `@Transactional` block as the domain change.
That removes the dual-write problem entirely, and with it the outbox table, the relay job, the
reconciliation task, and the eventual-consistency window on a security decision.

`OidfServiceIntegration` and `NotifyService` communicate with OIDF federation nodes; neither consumes
authorization decisions. No second deployable needs these answers.

## Design

### Principle: store only the exceptions

The foreign-key graph *is* the ownership relation set. `Subordinate → TaIm → Organization` already answers
"which org owns this". Duplicating that into relationship rows would create a synchronisation burden for
information the database already holds.

Two rule sources, one seam:

- **Implicit relations** — derived by walking existing JPA associations. No rows written, ever. No backfill.
- **Explicit relations** — stored rows, for what the FK graph cannot express.

### Schema

```sql
CREATE TABLE access_grant (
  grant_id    CHAR(36)    NOT NULL PRIMARY KEY,
  subject_org CHAR(36)    NOT NULL,
  object_type VARCHAR(40) NOT NULL,
  object_id   CHAR(36)    NOT NULL,
  relation    VARCHAR(40) NOT NULL,
  created_at  DATETIME    NOT NULL,
  CONSTRAINT uk_grant UNIQUE (subject_org, object_type, object_id, relation),
  CONSTRAINT fk_grant_org FOREIGN KEY (subject_org)
      REFERENCES organization (organization_id) ON DELETE CASCADE
);

CREATE INDEX ix_grant_lookup ON access_grant (subject_org, object_type, relation);
```

Follows the existing Flyway convention in `service/src/main/resources/db/migration/`.

Expected contents at launch: `registration.applicant` rows only. Everything else is implicit.

### Relation model

Expressed as an OpenFGA-style model for review purposes. This is the specification the Java resolver
implements; it is not fed to any server.

```
type organization

type federation_entity
  relations
    define owner: [organization]

type module                      # resolver | trustmark_issuer | ta_im
  relations
    define parent: [federation_entity, organization]
    define owner: owner from parent

type subordinate
  relations
    define parent: [module]
    define owner: owner from parent

type trust_mark
  relations
    define parent: [module]
    define owner: owner from parent

type registration
  relations
    define parent: [module]              # IM/TMI owner
    define applicant: [organization]     # party being registered
    define viewer: owner from parent or applicant
    define approver: owner from parent
```

`registration` is the case that justifies the work: two organizations, asymmetric rights.

### API

One interface, deliberately shaped to match OpenFGA's `Check` and `ListObjects` so the implementation can
be swapped without touching call sites.

```java
public interface AccessControl {

  /** Returns true if the organization holds the given relation on the object. */
  boolean check(OrganizationRecord org, Relation relation, ObjectRef object);

  /** Returns the ids of all objects of the given type on which the organization holds the relation. */
  Set<UUID> listObjects(OrganizationRecord org, Relation relation, ObjectType type);
}
```

Resolution:

- `check` — implicit owner match `OR` an `access_grant` row exists. One query, indexed.
- `listObjects` — the existing `WHERE o.orgNumber = :orgNumber` query `UNION` ids from `access_grant`.

At organization granularity the returned id set is bounded by what the current SQL already returns, so
`listObjects` is viable for list endpoints — no pagination problem.

Keep the whole implicit tree in a single `RelationResolver` class rather than scattering traversal logic
across services. The relation model must remain reviewable in one sitting.

### Enforcement

Enforce at the service boundary, not in `SecurityConfig`. Existing scope rules stay in place as a coarse
pre-filter — they are complementary, not redundant.

Roughly 10 object types × 4 operations of distinct logic behind 88 endpoints.

### Deletion

Implicit relations vanish with the owning row. Explicit grants are removed by `ON DELETE CASCADE` on
`subject_org`, and by an explicit delete when the object itself is removed.

## Work Breakdown

| # | Item | Estimate |
|---|------|----------|
| 1 | Flyway migration for `access_grant`; `AccessControl` interface, `RelationResolver`, SQL implementation | 1.5 days |
| 2 | Enforcement wiring across ~10 object types × 4 operations | 2 days |
| 3 | Fix `listRegistrationsConnectedToThisOrgIM` to use `listObjects`; remove the `findAll()` in-memory filter | 0.5 day |
| 4 | Test fixtures — grants seeded as plain JPA entities alongside existing fixtures | 1 day |

**Total: 4–6 days.**

Not required, and deliberately absent: OpenFGA client and model synchronisation, outbox table and relay,
backfill job, OpenFGA container in `config/local/docker-compose.yml`, production deployment of an
authorization service, model version pinning.

### Suggested sequencing

Land item 1 in shadow mode — evaluate `check`, log any disagreement with the existing `orgNumber` filter,
enforce nothing. Run it for a week against real traffic. Then switch item 2 to enforcing.

### Testing

Testcontainers is already configured (`service/src/test/java/se/swedenconnect/oidf/registry/fixture/TestContainersConfiguration.java`,
MariaDB). No new container is needed — grants are ordinary rows created by ordinary fixtures. The
existing integration suite and the `guitest` E2E suite need grant rows seeded wherever they currently
depend on organization scoping.

## Trade-offs

**Given up:**

- *Declarative model artifact.* Rules live in Java rather than a reviewable `.fga` file. Mitigated by
  confining the implicit tree to one `RelationResolver` class, and by keeping the model block above in
  sync with it.
- *Tooling.* No OpenFGA playground, CLI, or model test framework. Plain JUnit instead — adequate at this
  size.
- *Per-user or deep-nested grants.* Would require real work later: a recursive CTE or a general tuple
  table. This is deferred, not foreclosed.

**Gained:**

- No distributed-consistency failure mode on an authorization decision.
- No new runtime dependency to deploy, monitor, version or secure.
- Relationship changes are transactional with the domain changes that cause them.
- A `Registration` can finally express both of its organizations.

## Migration Path to OpenFGA

`AccessControl` is the only type the service layer depends on, and its two methods mirror OpenFGA's
`Check` and `ListObjects`. If per-user granularity or a second consuming service ever materialises, the
implementation is replaced and the roughly 40 call sites do not move.

The cost of keeping that door open is close to zero. The cost of not keeping it open is repeating this
refactor.

Triggers that would justify revisiting the decision:

- Access needs to be granted to individual users rather than organizations.
- A second deployable needs to answer the same authorization questions.
- Delegation chains need arbitrary depth rather than the fixed depth-3 hierarchy.
