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
| **Intermediate Administrator** | Operator with write rights on the Intermediate's owning organization who reviews, approves per step, or rejects registrations (`@orgRightsService.canRead/canWrite`) |
| **Federation Operator**        | Creates and assigns registration flows — to Intermediates, trust mark issuers, or individual trust marks                                                             |
| **System**                     | The registry service; runs the pipeline steps, creates subordinate statements and trust mark subjects                                                                |

---

## Preconditions

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
   belong to a tenant/org yet).
2. The member submits a join request: `POST /registration/v1/{tenant}/{orgNumber}/{joinId}` with:
    - `entityIdentifier` — the entity's URL
    - `trustmarksRequested` — desired trust marks, grouped by issuer (optional)
    - `metadata` — federation metadata for a hosted entity (optional; presence selects the hosted path)
3. **PRE — `InternalPreRegistrationStep`:** finds or creates a `Registration` (status `STARTED`, type `SUBORDINATE`)
   keyed by `entityId`. Fails the pipeline if a registration for this entity is already `PENDING_APPROVAL`.
4. **MID — hosted or remote path (mutually exclusive via `canApply`):**
    - `HostedEntityRegistrationStep` runs when the request body contains `metadata`: creates/updates the hosted entity (
      via `EntityConfigService`, so audit fires) and fetches the federation JWKS from the org's service node.
    - `LoadEntityConfigurationStep` runs otherwise: fetches and signature-validates the entity statement at `entityId`,
      extracting `metadata` and JWKS.
5. **MID — `TrustMarkIssuerRegistrationStep`** (only if trust marks were requested): for each requested trust mark type,
   resolves the assigned `TRUST_MARK_ISSUER` flow and dispatches a sub-pipeline (see UC-6). Trust marks with no assigned
   flow, or that don't exist, are skipped with a warning rather than failing the parent registration.
6. **POST — `PublishSubordinateStatementStep`:** creates (or updates, if one already exists for this entity +
   Intermediate) a subordinate statement with the loaded JWKS and metadata policy, and sets
   `Registration.status = APPROVED`.
7. The system returns a `RegistrationDto` with `successful = true`, `statusFedreg = APPROVED`, and the full `steps`
   execution trail.

**Result:** The entity is a subordinate of the Intermediate and visible in the federation. Any successfully processed
trust marks are reflected in `statusTrustmarks`.

### Alternative flow A — A step fails

- If a `PRE`/`MID`/`POST` step's `buildContext` or `execute` returns `FAILURE`, the pipeline aborts immediately (
  `ProcessReport.status = SKIPPED`).
- `Registration.status` is left as it was before the failing step (typically `STARTED`).
- The response has `successful = false`; `steps` lists which step failed and why (`StepIssue` entries with `Severity`).
- The member can fix the problem and resubmit — `POST` on the same `entityIdentifier` reuses the existing `Registration`
  row.

### Alternative flow B — Hosted vs. remote path

- If `metadata` is present in the request body, `HostedEntityRegistrationStep` runs and `LoadEntityConfigurationStep` is
  skipped (`SKIPPED` step result) — federation JWKS always comes from the service node, never from entity metadata.
- If `metadata` is absent, only `LoadEntityConfigurationStep` runs.

---

## UC-2: Submit registration request (manual review at any step)

**Actor:** Federation Member, Intermediate Administrator

**Goal:** The pipeline pauses at a configured checkpoint and waits for an operator to approve that specific step before
continuing.

### Main flow

1. Same as UC-1, steps 1–2.
2. The pipeline runs step by step. When it reaches a `MID` step configured with `manualreview=true` (any public `MID`
   step in the flow definition — not necessarily the last one), the engine:
    - still runs that step's `buildContext` (read-only validation),
    - then halts **before** `execute`, recording a `PENDING_APPROVAL` result for that step.
3. The system sets `Registration.status = PENDING_APPROVAL`,
   `Registration.pendingStepIndex = <index of the paused step>`, and persists the step results so far.
4. The system returns a `RegistrationDto` with `successful = false` and `statusFedreg = PENDING_APPROVAL`.
5. The Intermediate administrator sees the pending registration: `GET /registration-admin/v1/{tenant}/{orgNumber}` or
   `GET /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}`.
6. The administrator reviews the step results and context diff, then approves the pending step:
   `POST /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/steps/{stepIndex}/approve`. `stepIndex` must match
   `pendingStepIndex` exactly, or the call fails with `409 Conflict`.
7. On approval, the system reconstructs the pipeline context from the stored registration data (entity ID, JWKS,
   metadata policy, requested trust marks, request metadata) and resumes execution from `stepIndex` onward. The approved
   step's `buildContext`/approval gate is bypassed for this one step only (`STEP_APPROVED` context flag, consumed after
   `execute`).
8. If a later step in the same run also requires manual review, the pipeline pauses again with a new
   `pendingStepIndex` — steps 5–7 repeat.
9. Once all remaining steps complete, `PublishSubordinateStatementStep` runs and sets `Registration.status = APPROVED`.

**Result:** The entity becomes a subordinate once every gated step has been individually approved.

---

## UC-3: Reject a registration request

**Actor:** Intermediate Administrator

**Goal:** A registration currently paused for review is rejected with a stated reason.

### Main flow

1. The administrator opens a registration with `statusFedreg = PENDING_APPROVAL`.
2. The administrator rejects it: `POST /registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/reject` with
   `rejectionReason`.
3. The system sets `Registration.status = REJECTED`, stores `rejectionReason` and `reviewedAt`.
4. The system returns the updated `RegistrationDto`.
5. The registration record is deleted automatically 30 days later by `RegistrationCleanupJob` (nightly at 02:00,
   `deleteByStatusAndCreatedDateBefore(REJECTED, now-30d)`).

**Result:** The request is rejected. The member sees `rejectionReason` and may resubmit.

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
    - `steps` — an ordered list of selected `MID` steps, each with `config` key/value pairs (e.g.
      `manualreview: "true"`). If omitted for an `INTERMEDIATE` flow, the system defaults to
      `HostedEntityRegistrationStep` + `LoadEntityConfigurationStep`.
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
4. **POST — `CreateTrustMarkSubjectStep`:** runs only when the proceed signal is set. Creates a `TrustMarkSubject` (
   idempotent — a no-op if one already exists for this trust mark + subject), fires an audit event, and sets the child
   registration's status to `APPROVED`.
5. Each trust mark's outcome (`pending`, `failed`, `not found`, `no flow assigned`) is aggregated into the parent step's
   result message; a failure or pending trust mark does **not** fail the parent `SUBORDINATE` registration.
6. Approving a paused trust mark step uses the same admin endpoint as UC-2 (
   `.../{registrationId}/steps/{stepIndex}/approve}`), addressed by the **child** registration's ID —
   `RegistrationAdminServiceImpl.approveStep` detects `registrationType = TRUST_MARK_SUBORDINATE` and re-dispatches
   through the trust mark sub-flow instead of the parent flow.

**Result:** Each requested trust mark ends up as its own `TRUST_MARK_SUBORDINATE` registration with an independent
status, surfaced on the parent registration's `statusTrustmarks` (member view) or listed as its own row (admin view,
which does not filter out trust mark sub-registrations).

---

## Status reference

`RegistrationStatus` / `FedRegStatus` (identical value sets — the latter is the API-facing DTO enum):

| Status             | Meaning                                                      |
|--------------------|--------------------------------------------------------------|
| `STARTED`          | Registration created; pipeline has not yet completed         |
| `PENDING_APPROVAL` | Paused at a `manualreview=true` step; see `pendingStepIndex` |
| `APPROVED`         | Subordinate statement (or trust mark subject) created        |
| `REJECTED`         | Rejected by an operator; auto-deleted after 30 days          |

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

  A step FAILURE at any point aborts the run; status stays STARTED (or unchanged).
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
| `PublishSubordinateStatementStep`                                                 | POST (fixed) | —                     | Creates or updates the subordinate statement from the loaded JWKS/metadata policy and sets status `APPROVED`.                                                          |
| `MetadataPolicyCreationStep`, `RpMetadataValidationStep`, `TrustMarkApprovalStep` | MID          | No (`isPublic=false`) | Defined but not currently selectable via the flow-builder UI/API, and not part of the default flow.                                                                    |

### TRUST_MARK_ISSUER flow steps

| Step                                   | Type         | Public / selectable | Description                                                                                                              |
|----------------------------------------|--------------|---------------------|--------------------------------------------------------------------------------------------------------------------------|
| `InternalPreTrustMarkRegistrationStep` | PRE (fixed)  | —                   | Finds or creates the child `TRUST_MARK_SUBORDINATE` registration linked to its parent.                                   |
| `AddTrustMarkSubjectStep`              | MID          | Yes                 | Validates the requested trust mark type exists; supports `manualreview` to gate subject creation.                        |
| `CreateTrustMarkSubjectStep`           | POST (fixed) | —                   | Creates the `TrustMarkSubject` and marks the child registration `APPROVED`. Runs only if the MID step signalled proceed. |

Every `MID` step declares its own `manualreview` (boolean, default `false`) config value; `PRE`/`POST` steps always run
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
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}`                                                    | List registrations connected to this org's Intermediates                  |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}`                                   | Get a specific registration (admin)                                       |
| `GET`    | `/registration-admin/v1/{tenant}/{orgNumber}/count?taimId=`                                      | Count unhandled `PENDING_APPROVAL` registrations for an Intermediate      |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/reject`                            | Reject a pending registration                                             |
| `POST`   | `/registration-admin/v1/{tenant}/{orgNumber}/{registrationId}/steps/{stepIndex}/approve`         | Approve the pending step and resume the pipeline                          |
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
