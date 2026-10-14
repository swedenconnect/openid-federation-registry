# Use Case: Registration Flow

## Overview

The registration flow handles how an entity joins the federation through an Intermediate, and — optionally — how it
enrolls for trust marks in the same request. Registration is driven by a configurable, ordered pipeline of **steps** (
`PRE` → `MID` → `POST`) executed by a `ProcessEngine`. Any `MID` step can be configured with `manualreview=true`, which
pauses the pipeline for operator approval; approval is granted per step, not just once at the end. A registration that
requests trust marks spawns one child registration per trust mark type, each running its own sub-pipeline.

There are two flow types (`Step.FlowType`):

- **INTERMEDIATE** — the flow a federation member submits to when joining via an Intermediate (`TaIm`). Configured with
  `RegistrationFlowDto.flowType = INTERMEDIATE` and assigned to a `TrustAnchorIntermediateModule`.
- **TRUST_MARK_ISSUER** — the sub-flow run for each requested trust mark. Assigned either to a `TrustMarkIssuer` (all
  trust marks from that issuer) or to a specific `TrustMark`.

---

## Actors

| Actor                          | Description                                                                                                                                                          |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Federation Member**          | An organization, scoped to a tenant, that wants to connect an entity to the federation (`entityIdentifier` + `orgNumber` + `tenant`)                                 |
| **Tenant Operator**            | An organization listed in the tenant's `operator_organizations` (or a superuser). Reviews, approves per step, or rejects every registration on the tenant, whichever organization owns the Intermediate (`@orgRightsService.canReadAsOperator/canWriteAsOperator`) |
| **Federation Operator**        | Creates and assigns registration flows — to Intermediates, trust mark issuers, or individual trust marks                                                             |
| **System**                     | The registry service; runs the pipeline steps, creates subordinate statements and trust mark subjects                                                                |

---

## Preconditions

- **UC-0: the member has bootstrapped its organization and claimed the entity's domain** (see
  [Organizations and Domains](organization.md)). The member calls `POST /organization/v1/{tenant}/{orgNumber}`
  with a `legalName`, then `POST /organization/v1/{tenant}/{orgNumber}/domains` with the hostname of the entity
  identifiers. A `PENDING` claim is enough. Without a claim, every registration request below returns `400` (see
  [domain enforcement](organization.md#domain-enforcement-on-registration-requests)). To disable the check, set
  `openid.federation.registry.registration.require-registered-domain=false`.
- The federation member is authenticated and authorized for the target `tenant`/`orgNumber` (
  `@orgRightsService.canWrite`).
- The entity to register either:
    - has a valid, signature-verified entity statement reachable at its `entityId` (non-hosted path), **or**
    - is hosted in this registry, in which case `metadata` is supplied in the join request body (hosted path).
- A `RegistrationFlow` of type `INTERMEDIATE` exists and is assigned to the target Intermediate via a `FlowAssignment`.
- The federation member knows the `joinId` (the `FlowAssignment.assignId`) for the flow they want to use.
- For trust mark enrollment: a `RegistrationFlow` of type `TRUST_MARK_ISSUER` is assigned to the relevant
  `TrustMarkIssuer` or `TrustMark` via `TrustMarkIssuerFlowAssignment` / `TrustMarkFlowAssignment`.

---

## UC-1: Submit registration request (automatic flow)

**Actor:** Federation Member

**Goal:** The entity is registered and a subordinate statement is published automatically, without manual review.

### Main flow

1. The member lists available flows: `GET /registration/v1/flows` (unscoped — an applicant browsing flows need not
   belong to a tenant/org yet). Only assignments of **enabled** flows are listed. Flows the operator has disabled
   (UC-5) are not offered.
2. The member submits a join request: `POST /registration/v1/{tenant}/{orgNumber}/{joinId}` with:
    - `entityIdentifier` — the entity's URL
    - `trustmarksRequested` — desired trust marks, grouped by issuer (optional)
    - `metadata` — federation metadata for a hosted entity (optional; presence selects the hosted path)
3. **Before the pipeline runs:** the system checks that the host of `entityIdentifier` equals, or is a subdomain
   of, one of the member's `PENDING`/`VALIDATED` domains. If not, it returns `400`.
4. **Before the pipeline runs:** the system checks that the flow behind `joinId` is enabled. If not, it returns
   `409 Conflict` (`Registration flow is disabled: <name>`) and creates no `Registration` row. The same check
   applies to the re-run path (`PUT /registration/v1/{tenant}/{orgNumber}/{registrationId}`), which runs the flow
   again under the existing assignment.
5. **PRE — `InternalPreRegistrationStep`:** finds or creates a `Registration` (status `STARTED`, type `SUBORDINATE`)
   keyed by `entityId`. Fails the pipeline if a registration for this entity is already `PENDING_APPROVAL`.
6. **MID — hosted or remote path (mutually exclusive via `canApply`):**
    - `HostedEntityRegistrationStep` runs when the request body contains `metadata`: creates/updates the hosted entity (
      via `EntityConfigService`, so audit fires) and fetches the federation JWKS from the org's service node.
    - `LoadEntityConfigurationStep` runs otherwise: fetches and signature-validates the entity statement at `entityId`,
      extracting `metadata` and JWKS.
7. **MID — `TrustMarkIssuerRegistrationStep`** (only if trust marks were requested): for each requested trust mark type,
   resolves the assigned `TRUST_MARK_ISSUER` flow and dispatches a sub-pipeline (see UC-6). A trust mark with no
   assigned flow, with a disabled flow, or that doesn't exist is settled at once: approved if the entity already holds
   it (a trust mark subject that is not revoked), otherwise declined with the reason. The parent registration
   continues.
8. **POST — `PublishSubordinateStatementStep`:** creates (or updates, if one already exists for this entity +
   Intermediate) a subordinate statement with the loaded JWKS and metadata policy, and sets
   `Registration.status = APPROVED`.
9. The system returns a `RegistrationDto` with `successful = true`, `statusFedreg = APPROVED`, and the full `steps`
   execution trail.

**Result:** The entity is a subordinate of the Intermediate and visible in the federation. Any successfully processed
trust marks are reflected in `statusTrustmarks`.

### Alternative flow A — A step fails

- If a `PRE`/`MID`/`POST` step's `buildContext` or `execute` returns `FAILURE`, the pipeline aborts immediately (
  `ProcessReport.status = SKIPPED`).
- The registration is declined automatically: `Registration.status = REJECTED`. It does not wait for a review that
  never comes.
  - `rejectionReason`, shown to the member: the failed step's `userdeclinereason`, or when the flow gives none
    `Automatically declined: '<name>' failed.`, where `<name>` is the step's `userdisplayname_en`, or
    `an automated check` for a step not shown for the user. No technical details.
  - `technicalReason`, for operators: the failed step, its message and its issues. Only in the registration admin API
    and the registry GUI, never in the member API.
- Each issue has a `type`: `VALIDATION`, something the entity registers that is not accepted, which the member is shown
  and can fix; or `TECHNICAL`, something the member cannot fix from the registration, such as an entity configuration
  or key set that could not be fetched, resolved with an operator. The portal shows validation messages, and for a
  technical issue only that support has to be contacted.
- `LoadEntityConfigurationStep` fails the step, rather than the request, when the entity configuration cannot be
  fetched (`TECHNICAL`) or is not valid (`VALIDATION`).
- Exception: a request for an entity whose registration is `PENDING_APPROVAL` is refused by the `PRE` step and leaves
  that registration as it is, with its paused step and saved context.
- The response has `successful = false`; `steps` lists which step failed and why (`StepIssue` entries with `Severity`).
- The member can fix the problem and resubmit — `POST` on the same `entityIdentifier` reuses the existing `Registration`
  row.

### Alternative flow B — Hosted vs. remote path

- If `metadata` is present in the request body, `HostedEntityRegistrationStep` runs and `LoadEntityConfigurationStep` is
  skipped (`SKIPPED` step result) — federation JWKS always comes from the service node, never from entity metadata.
- If `metadata` is absent, only `LoadEntityConfigurationStep` runs.

---

## UC-2: Submit registration request (manual review at any step)

**Actor:** Federation Member, Tenant Operator

**Goal:** The pipeline pauses at a configured checkpoint and waits for an operator to approve that specific step before
continuing.

### Main flow

1. Same as UC-1, steps 1–2.
2. The pipeline runs step by step. When it reaches a `MID` step configured with `manualreview=true` (any public `MID`
   step in the flow definition — not necessarily the last one), the engine:
    - still runs that step's `buildContext`, which loads everything the step needs without writing anything,
    - then halts **before** `execute`, recording a `PENDING_APPROVAL` result for that step together with the step's
      **preview**: typed sections (`FIELDS`, `CHECKS`, `JSON`, `LIST`, `TEXT`, `IMAGE`, `DIFF`) describing what the
      step is about to do. Only steps implementing `ReviewableStep` can pause, and every step that offers
      `manualreview` must implement it — the application refuses to start otherwise.
3. The system sets `Registration.status = PENDING_APPROVAL`,
   `Registration.pendingStepIndex = <index of the paused step>`, persists the step results so far, and saves the
   context the steps before the pause produced (entity metadata, federation keys, metadata policy, request metadata)
   in `Registration.processContext`.
4. The system returns a `RegistrationDto` with `successful = false` and `statusFedreg = PENDING_APPROVAL`.
5. The tenant operator sees the pending registration: `GET /registration-admin/v1/{tenant}/{orgNumber}` or
   `GET /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}`.
6. The operator reviews the step's preview and the step results, then approves the pending step:
   `POST /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/steps/{stepIndex}/approve`. `stepIndex` must match
   `pendingStepIndex` exactly, or the call fails with `409 Conflict`.
7. On approval, the system rebuilds the pipeline context from the registration (entity ID, organization, flow,
   requested trust marks) and the saved `processContext`, and resumes execution from `stepIndex` onward. The approved
   step's `buildContext`/approval gate is bypassed for this one step only (`STEP_APPROVED` context flag, consumed after
   `execute`), so the step runs on exactly the data the operator reviewed. The approved step's result records
   `reviewedBy` and `reviewedAt`, the registration's `reviewedBy`/`reviewedAt` are updated, and a
   `REGISTRATION_STEP_APPROVED` audit event is emitted.
8. If a later step in the same run also requires manual review, the pipeline pauses again with a new
   `pendingStepIndex` — steps 5–7 repeat.
9. Once all remaining steps complete, `PublishSubordinateStatementStep` runs and sets `Registration.status = APPROVED`.
10. If a step after the approved one fails, the registration is declined automatically, as a failed first run is
    (Alternative flow A); the failed step and its issues are in the step results.

**Result:** The entity becomes a subordinate once every gated step has been individually approved.

---

## UC-2b: Review an entity

**Actor:** Tenant Operator

**Goal:** Decide everything one entity waits for in one session, in a fixed order: the **domain** it is registered
under, then the **trust marks** it asked for, then the steps of its own **registration** that need review.

An entity is identified by its own registration (`registrationId` of the `SUBORDINATE` registration). The items are
not stored; they are read from the domains and registrations each time:

| Item         | What it is                                                                                    |
|--------------|-----------------------------------------------------------------------------------------------|
| `DOMAIN`     | Each domain of the entity's organization that covers the entity's host.                       |
| `TRUST_MARK` | Each trust mark registration under the entity's registration that is paused or decided.      |
| `ENTITY`     | The steps of the entity's registration approved so far, the paused step, and the later steps that will pause too (`UPCOMING`). |

In the admin UI this is **Registrations → Entities**: the entity list, an entity page listing its items in order,
and a review view with the item's preview and **Reject… / Defer decision / Approve and continue**. **Registrations →
Domain claims** still lists every domain claim, including those no entity is registered under yet.

### Main flow

1. The operator lists the entities: `GET /registration-admin/v1/{tenant}/{orgNumber}/entity-reviews` (`?all=true`
   includes entities with nothing left to review). Each row counts the pending domains, trust marks and entity steps
   and says whether a review is `NOT_STARTED`, `IN_REVIEW` or `COMPLETED`.
2. The operator opens an entity: `GET .../entity-reviews/{registrationId}` returns the items in review order, each with
   its state (`PENDING`, `WAITING`, `UPCOMING`, `APPROVED`, `REJECTED`) and the preview the operator reviews.
3. The operator starts the review: `POST .../entity-reviews/{registrationId}/start`. If a review is already open it is
   returned instead, so any operator of the tenant can continue a review another started.
4. For each item the operator posts a decision, `POST .../entity-reviews/{registrationId}/decisions` with
   `itemType`, `itemId`, `stepIndex` (for a registration, as listed) and `decision`:
    - `APPROVED` and `REJECTED` (with `reason`) go through the same service as the single-item endpoints, with the same
      checks, cascades and audit events. Approving an entity step resumes the flow, which may pause at the next
      review step; that step then appears as the next item.
    - `DEFERRED` records the deferral and changes nothing else. The review stays open and the entity is listed as
      `IN_REVIEW` ("Continue review").
5. When nothing is left to decide, the review is `COMPLETED`.

### Order

An item is `WAITING`, and cannot be approved, until the items before it are decided:

- a trust mark or entity step waits while a domain covering the entity is `PENDING` and none is `VALIDATED`;
- an entity step also waits while a trust mark it asked for is pending.

The rule is enforced by the server, also on `.../{registrationId}/steps/{stepIndex}/approve`, which answers `409`
with the reason.

### Shared domains

A domain covers every entity of the organization under it, so it shows in each of their reviews. It is decided once:
the other reviews show it as decided, with `decidedInReviewOf` naming the entity in whose review it was decided.
Deciding a domain does not start a review of the other entities.

### Rejections

- Rejecting the domain rejects the entities under it that no other domain of the organization covers, with reason
  "Not Accepted Domain" (see UC-3), and their reviews end.
- Rejecting a trust mark rejects only that trust mark; the review continues.
- Rejecting an entity step rejects the registration and ends the review.

---

## UC-3: Reject a registration request

**Actor:** Tenant Operator

**Goal:** A registration currently paused for review is rejected with a stated reason.

### Main flow

1. The operator opens a registration with `statusFedreg = PENDING_APPROVAL`.
2. The operator rejects it: `POST /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/reject` with
   `rejectionReason`.
3. The system sets `Registration.status = REJECTED`, stores `rejectionReason`, `reviewedAt` and `reviewedBy`, drops the
   saved `processContext`, and emits a `REGISTRATION_REJECTED` audit event.
4. The system returns the updated `RegistrationDto`.
5. The registration record is deleted automatically 30 days later by `RegistrationCleanupJob` (nightly at 02:00,
   `deleteByStatusAndCreatedDateBefore(REJECTED, now-30d)`).

**Result:** The request is rejected. The member sees `rejectionReason` and may resubmit.

### Alternative flow: rejected by a domain rejection

When the tenant operator rejects a domain
(`POST /registration-admin/v1/{tenant}/{orgNumber}/domains/{domainId}/reject`), the registrations under it are
rejected too. This applies to each `STARTED` or `PENDING_APPROVAL` registration of the organization whose entity
host is covered by the rejected domain and by no other `PENDING`/`VALIDATED` domain of the organization. Each
gets status `REJECTED` and the exact reason `Not Accepted Domain`. Its unsettled `TRUST_MARK_SUBORDINATE`
children get the same. The response lists all of them in `cascadedRegistrationIds`. See
[Organizations and Domains](organization.md#cascading-a-domain-rejection).

### Error flow

- If the registration is not currently `PENDING_APPROVAL`, the system returns `409 Conflict`.

---

## UC-4: Withdraw / delete a registration

**Actor:** Federation Member

**Goal:** Remove a registration request, at any stage — including an already-approved one.

### Main flow

1. The member lists their registrations: `GET /registration/v1/{tenant}/{orgNumber}`.
2. The member deletes one: `DELETE /registration/v1/{tenant}/{orgNumber}/{registrationId}`.
3. If the registration is `APPROVED`, the system also deletes the matching subordinate statement(s) for that entity +
   Intermediate.
4. The system deletes any hosted-entity record for that `entityId` owned by the member's organization.
5. The system deletes the `Registration` row.

**Result:** The registration (and, if it existed, the associated subordinate statement and hosted entity) is removed.

> Unlike earlier versions of this flow, `APPROVED` registrations are **not** protected from deletion — deleting one
> cascades to the subordinate statement and any hosted entity.

---

## UC-5: Configure a registration flow

**Actor:** Federation Operator

**Goal:** Create a registration flow and assign it to an Intermediate, a trust mark issuer, or a specific trust mark.

### Main flow

1. The operator lists selectable pipeline steps: `GET /registration-flow/v1/{tenant}/{orgNumber}/steps` — only steps
   marked `isPublic() = true` and `stepType = MID` are returned; `PRE`/`POST` framework steps are injected automatically
   per flow type and are not user-configurable.
2. The operator creates a flow: `POST /registration-flow/v1/{tenant}/{orgNumber}/flow` (or with a specified ID via the
   `/flow/{flowid}` variant) with:
    - `name`, `description`, `descriptionSv`, `technology` (`OIDC`/`SAML`), `entityType`
    - `flowType` (`INTERMEDIATE` or `TRUST_MARK_ISSUER`)
    - `enabled`: whether the flow can be used. Optional, defaults to enabled. `PUT .../flow/{flowid}` replaces the
      whole flow with the same default, so a client must send `enabled: false` on every update to keep a flow
      disabled.
    - `steps` — an ordered list of selected `MID` steps, each with `config` key/value pairs (e.g.
      `manualreview: "true"`). If omitted for an `INTERMEDIATE` flow, the system defaults to
      `HostedEntityRegistrationStep` + `LoadEntityConfigurationStep`.
    - Every selectable step also has `showforuser` (boolean, default `false`), `userdisplayname_sv`,
      `userdisplayname_en`, `userdescription_sv` and `userdescription_en`: whether the step is shown to the user who
      registers, and the name and description the user sees, in Swedish and English. With `showforuser` set, all four
      texts are required, or the flow is refused with `400`. The step results of a registration (`steps` in
      `RegistrationDto`) carry the texts as `displayName`, `displayNameSv`, `displayDescription` and
      `displayDescriptionSv` for the steps shown for the user, so the member's view can name the steps in plain
      language.
    - Every selectable step also has `userdeclinereason`: the reason the member sees when the step fails and the
      registration is declined. Empty gives a generic reason, see Alternative flow A.
3. The system saves the flow and returns the flow DTO (with `flowId`).
4. The operator assigns the flow:
    - to an Intermediate: `POST /registration-flow/v1/{tenant}/{orgNumber}/intermediate/{taImId}/assign`
    - to a trust mark issuer: `POST /registration-flow/v1/{tenant}/{orgNumber}/trustmark-issuer/{tmIssuerId}/assign`
    - to a specific trust mark: `POST /registration-flow/v1/{tenant}/{orgNumber}/trustmark/{trustmarkId}/assign`

   Each is idempotent — re-assigning the same flow to the same target returns the existing `assignId`.
5. The system creates the assignment and returns `assignId` (this is the `joinId` federation members use to apply, for
   Intermediate assignments).

**Result:** The flow is assigned; `assignId`/`joinId` can be distributed. Flows, steps, and assignments can also be
updated (`PUT .../flow/{flowid}`) or removed (`DELETE .../flow/{flowid}`, `DELETE .../{type}/{id}/assign/{assignId}`).

### Taking a flow out of service

Setting `enabled = false` takes a flow out of service without deleting or unassigning it. Use it to pause or
replace a flow. A disabled flow has these effects:

- `GET /registration/v1/flows` no longer lists the flow's assignments.
- `POST /registration/v1/{tenant}/{orgNumber}/{joinId}` and the `PUT` re-run path return `409 Conflict`
  (`Registration flow is disabled: <name>`) and create nothing (UC-1).
- A disabled `TRUST_MARK_ISSUER` flow counts as unassigned. The trust mark request is declined, unless the entity
  already holds the trust mark, and the parent registration completes (UC-1, UC-6).
- **Registrations already submitted are unaffected.** They can still be approved step by step (UC-2) or rejected
  (UC-3).
- `GET /registration-flow/v1/{tenant}/{orgNumber}/flows` still lists the flow with `enabled = false`, so the
  operator can enable it again.

---

## UC-6: Trust mark enrollment sub-flow

**Actor:** System (triggered from within UC-1/UC-2), Intermediate/Trust-mark Administrator

**Goal:** For each trust mark requested alongside a subordinate registration, run an independent enrollment pipeline
and, once approved, create a `TrustMarkSubject`.

### Main flow

1. `TrustMarkIssuerRegistrationStep` (a `MID` step in the parent `INTERMEDIATE` flow) iterates the requested trust
   marks. For each type it resolves the `TrustMarkFlowAssignment` and builds a sub-pipeline:
   `[PRE auto] + [configured MID steps] + [POST auto]`.
2. **PRE — `InternalPreTrustMarkRegistrationStep`:** finds or creates a child `Registration` with
   `registrationType = TRUST_MARK_SUBORDINATE`, `entityId = <trustmarkType>`,
   `parentRegistration = <the SUBORDINATE registration>`, status `STARTED`.
3. **MID — `AddTrustMarkSubjectStep`:** validates that the requested trust mark type exists for the given issuer (
   `buildContext`; `FAILURE` aborts this trust mark's sub-flow only). If `manualreview=true` on this step, the sub-flow
   pauses the same way as UC-2, with its own `pendingStepIndex` on the child registration. Otherwise it signals
   `TRUSTMARK_SUBJECT_PROCEED` and continues.
    - **Exception: the organization is pre-validated for this type.** The sub-flow does not pause if the
      organization holds the requested `trustmarkType` among its pre-validated trust marks
      (`PUT /registration-admin/v1/{tenant}/{orgNumber}/organizations/{targetOrgNumber}/trustmarks`).
      `TrustMarkIssuerRegistrationStep` sets `ContextKey.TRUST_MARK_PRE_VALIDATED` on the sub-flow context. The
      engine then passes the gated step and records a `WARNING` result: *"Auto-approved: organization holds a
      pre-validated trust mark for this enrollment"*. It does not return `pendingApproval`. The flow, steps, child
      registration and step trail are unchanged. The parent registration's context never gets the flag. See
      [Organizations and Domains](organization.md#effect-on-trust-mark-enrollment).
4. **POST — `CreateTrustMarkSubjectStep`:** runs only when the proceed signal is set. Creates a `TrustMarkSubject` (
   idempotent — a no-op if one already exists for this trust mark + subject), fires an audit event, and sets the child
   registration's status to `APPROVED`.
5. Each trust mark's outcome (`pending`, `failed`, `declined`, `already held`) is aggregated into the parent step's
   result message; a failure, pending or declined trust mark does **not** fail the parent `SUBORDINATE` registration.
   A trust mark that has no usable flow gets a `TRUST_MARK_SUBORDINATE` registration without running a sub-flow:
   `APPROVED` if the entity already holds the trust mark, otherwise `REJECTED` with the reason
   `Automatically declined: the trust mark cannot be requested.` Why (no flow assigned, a disabled flow, or an unknown
   trust mark) is in `technicalReason` and the log, not shown to the member. A trust mark whose sub-flow fails is declined automatically the same way, with the
   reason naming the failed step as in Alternative flow A.
6. A domain rejection also ends a paused trust mark sub-flow. When a domain rejection rejects the parent
   `SUBORDINATE` registration, its unsettled `TRUST_MARK_SUBORDINATE` children are rejected with reason
   `Not Accepted Domain`. See
   [Organizations and Domains](organization.md#cascading-a-domain-rejection).
7. Approving a paused trust mark step uses the same admin endpoint as UC-2 (
   `.../{registrationId}/steps/{stepIndex}/approve}`), addressed by the **child** registration's ID —
   `RegistrationAdminServiceImpl.approveStep` detects `registrationType = TRUST_MARK_SUBORDINATE` and re-dispatches
   through the trust mark sub-flow instead of the parent flow. The pre-validation flag is set again when that
   context is rebuilt, so a later gated step in the same sub-flow also passes.

**Result:** Each requested trust mark ends up as its own `TRUST_MARK_SUBORDINATE` registration with an independent
status, surfaced on the parent registration's `statusTrustmarks` (member view, with `rejectionReason` for a rejected
trust mark) or listed as its own row (admin view, which does not filter out trust mark sub-registrations).

---

## Status reference

`RegistrationStatus` / `FedRegStatus` (identical value sets — the latter is the API-facing DTO enum):

| Status             | Meaning                                                      |
|--------------------|--------------------------------------------------------------|
| `STARTED`          | Registration created; pipeline has not yet completed         |
| `PENDING_APPROVAL` | Paused at a `manualreview=true` step; see `pendingStepIndex` |
| `APPROVED`         | Subordinate statement (or trust mark subject) created        |
| `REJECTED`         | Rejected by an operator, or declined automatically when a step failed; auto-deleted after 30 days |

`ProcessStatus` (pipeline run outcome, not persisted on `Registration` directly):

| Status             | Meaning                                                   |
|--------------------|-----------------------------------------------------------|
| `COMPLETED`        | All steps ran; check individual step results for warnings |
| `SKIPPED`          | Aborted early because a step returned `FAILURE`           |
| `PENDING_APPROVAL` | Paused at a manual-review gate                            |
| `FAILED`           | Defined but not currently produced by the engine          |

`StepStatus` (per-step outcome): `SUCCESS`, `SKIPPED` (`canApply` returned false), `WARNING`, `FAILURE` (aborts the
pipeline), `PENDING_APPROVAL`.

### Status flow

```
                         ┌─────────┐
     [Join request]  ──► │ STARTED │
                         └────┬────┘
                              │  pipeline runs
              ┌───────────────┼────────────────────┐
              │                                     │
   no manualreview step hit            a MID step has manualreview=true
              │                                     │
              ▼                                     ▼
        ┌──────────┐                     ┌───────────────────┐
        │ APPROVED │                     │ PENDING_APPROVAL   │◄─┐
        └──────────┘                     └─────────┬──────────┘  │ another gated
                                                     │             │ step reached
                                     admin approves  │             │ after resume
                                     pendingStepIndex ▼            │
                                          resume remaining steps ──┘
                                                     │
                                        all steps complete
                                                     │
                                    ┌────────────────┼───────────────┐
                                    ▼                                ▼
                              ┌──────────┐                    ┌──────────┐
                              │ APPROVED │        admin rejects│ REJECTED │
                              └──────────┘        (any pending)└────┬─────┘
                                                                     │
                                                        [Deleted after 30 days]

  A step FAILURE at any point aborts the run and declines the registration (REJECTED).
```

---

## Pipeline steps

### INTERMEDIATE flow steps

| Step                                                                              | Type         | Public / selectable   | Description                                                                                                                                                            |
|-----------------------------------------------------------------------------------|--------------|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `InternalPreRegistrationStep`                                                     | PRE (fixed)  | —                     | Finds or creates the `Registration` row. Fails if a `PENDING_APPROVAL` registration already exists for the entity.                                                     |
| `HostedEntityRegistrationStep`                                                    | MID          | Yes (default)         | Runs only when the join request contains `metadata`. Creates/updates the hosted entity and loads federation JWKS from the org's service node. Supports `manualreview`. |
| `LoadEntityConfigurationStep`                                                     | MID          | Yes (default)         | Runs only when `metadata` is absent. Fetches and signature-validates the entity statement at `entityId`. Supports `manualreview`.                                      |
| `TrustMarkIssuerRegistrationStep`                                                 | MID          | Yes                   | Dispatches a `TRUST_MARK_ISSUER` sub-pipeline for each requested trust mark (see UC-6).                                                                                |
| `PredefinedDirectRegisterFlow`                                                    | MID          | Yes (legacy)          | Combines `LoadEntityConfigurationStep` + `PublishSubordinateStatementStep` into a single selectable step.                                                              |
| `PublishSubordinateStatementStep`                                                 | POST (fixed) | —                     | Creates or updates the subordinate statement from the loaded JWKS, metadata and metadata policy and sets status `APPROVED`. Never pauses for review.                   |
| `MetadataReviewStep`                                                              | MID          | Yes                   | Shows the operator the entity's metadata (logo, names, links, changes) and always waits for approval. See [Metadata Review and Metadata Policy](metadata_policy.md). |
| `MetadataPolicyCreationStep`                                                      | MID          | Yes                   | Builds the `metadata` and `metadata_policy` claims from a template and checks the metadata against it. Supports `manualreview`. See [Metadata Review and Metadata Policy](metadata_policy.md). |
| `RpMetadataValidationStep`, `TrustMarkApprovalStep`                               | MID          | No (`isPublic=false`) | Defined but not currently selectable via the flow-builder UI/API, and not part of the default flow.                                                                    |

### TRUST_MARK_ISSUER flow steps

| Step                                   | Type         | Public / selectable | Description                                                                                                              |
|----------------------------------------|--------------|---------------------|--------------------------------------------------------------------------------------------------------------------------|
| `InternalPreTrustMarkRegistrationStep` | PRE (fixed)  | —                   | Finds or creates the child `TRUST_MARK_SUBORDINATE` registration linked to its parent.                                   |
| `AddTrustMarkSubjectStep`              | MID          | Yes                 | Validates the requested trust mark type exists; supports `manualreview` to gate subject creation.                        |
| `CreateTrustMarkSubjectStep`           | POST (fixed) | —                   | Creates the `TrustMarkSubject` and marks the child registration `APPROVED`. Runs only if the MID step signalled proceed. |

Every `MID` step that can pause declares its own `manualreview` (boolean, default `false`) config value and implements
`ReviewableStep`, which supplies the preview shown to the reviewer; `PRE`/`POST` steps always run
with their default config and are not individually configurable in the flow definition. The pipeline aborts at the first
`FAILURE`; a step can also return `WARNING` (pipeline continues) or be `SKIPPED` when its `canApply` check is false.

---

## Related API endpoints

All endpoints are scoped under `/{tenant}/{orgNumber}` except where noted, and gated by `@orgRightsService.canRead`/
`canWrite` for that org.

| Method   | Endpoint                                                                                         | Description                                                               |
|----------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `GET`    | `/registration/v1/flows`                                                                         | List all available registration flows (unscoped)                          |
| `GET`    | `/registration/v1/{tenant}/{orgNumber}`                                                          | List own registrations                                                    |
| `GET`    | `/registration/v1/{tenant}/{orgNumber}/{registrationId}`                                         | Get a specific registration                                               |
| `POST`   | `/registration/v1/{tenant}/{orgNumber}/{joinId}`                                                 | Submit a registration (join) request                                      |
| `PUT`    | `/registration/v1/{tenant}/{orgNumber}/{registrationId}`                                         | Re-run the flow for an existing entity                                    |
| `DELETE` | `/registration/v1/{tenant}/{orgNumber}/{registrationId}`                                         | Remove a registration (cascades to subordinate/hosted entity if approved) |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}`                                                    | List registrations on the tenant (operator)                               |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews`                                     | List entities with what each waits for (UC-2b)                            |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews/{registrationId}`                    | Get an entity's review items in order (UC-2b)                             |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews/{registrationId}/start`              | Start or continue the review of an entity (UC-2b)                         |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews/{registrationId}/decisions`          | Approve, reject or defer one review item (UC-2b)                          |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}`                                   | Get a specific registration (admin)                                       |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/reject`                            | Reject a pending registration                                             |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/steps/{stepIndex}/approve`         | Approve the pending step and resume the pipeline                          |
| `POST`   | `/organization/v1/{tenant}/{orgNumber}`                                                          | Bootstrap the organization's registry record (UC-0)                       |
| `POST`   | `/organization/v1/{tenant}/{orgNumber}/domains`                                                  | Claim a domain to register entities under (UC-0)                          |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/domains?status=`                                    | List domain claims on this tenant (operator)                              |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/domains/{domainId}/approve`                         | Approve a pending domain claim                                            |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/domains/{domainId}/reject`                          | Reject a domain claim and cascade to dependent registrations              |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/flows`                                               | List registration flows owned by this org                                 |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/steps`                                               | List selectable (`public`, `MID`) pipeline steps                          |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/flow/{flowId}`                                       | Get a flow                                                                |
| `POST`   | `/registration-flow/v1/{tenant}/{orgNumber}/flow`                                                | Create a flow (random ID)                                                 |
| `POST`   | `/registration-flow/v1/{tenant}/{orgNumber}/flow/{flowid}`                                       | Create a flow with a specified ID                                         |
| `PUT`    | `/registration-flow/v1/{tenant}/{orgNumber}/flow/{flowid}`                                       | Update a flow                                                             |
| `DELETE` | `/registration-flow/v1/{tenant}/{orgNumber}/flow/{flowid}`                                       | Delete a flow                                                             |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/intermediate/{taImId}/flows`                         | List flows assigned to an Intermediate                                    |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/intermediate/{taImId}/assignments`                   | List flow assignments for an Intermediate                                 |
| `POST`   | `/registration-flow/v1/{tenant}/{orgNumber}/intermediate/{taImId}/assign`                        | Assign a flow to an Intermediate                                          |
| `DELETE` | `/registration-flow/v1/{tenant}/{orgNumber}/intermediate/{taImId}/assign/{assignId}`             | Remove an Intermediate flow assignment                                    |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark-issuer/{tmIssuerId}/assignments`           | List flow assignments for a trust mark issuer                             |
| `POST`   | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark-issuer/{tmIssuerId}/assign`                | Assign a flow to a trust mark issuer                                      |
| `DELETE` | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark-issuer/{tmIssuerId}/assign/{assignId}`     | Remove a trust mark issuer flow assignment                                |
| `GET`    | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark-issuer/{tmIssuerId}/trustmark-assignments` | List flow assignments for all trust marks under an issuer                 |
| `POST`   | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark/{trustmarkId}/assign`                      | Assign a flow to a specific trust mark                                    |
| `DELETE` | `/registration-flow/v1/{tenant}/{orgNumber}/trustmark/{trustmarkId}/assign/{assignId}`           | Remove a trust mark flow assignment                                       |
