# Suggestion: Store Metadata from Join Request via EntityConfigService

## Problem

`RegistrationJoinRequestDto` has a `metadata` field (`Map<String, Object>`), but
`RegistrationFlowService.executeRegistrationFlow` never puts that value into the
`ProcessContext`. If a step were to persist metadata by writing directly to the
repository, `RegistryAuditService` would not be triggered because audit only fires
through `EntityConfigServiceImpl`.

## Goal

When `metadata` is non-null in the join request body, a dedicated step must call
`EntityConfigService.createHostedEntity` / `updateHostedEntity` so that
`auditService.hostedEntityCreated` / `hostedEntityUpdated` fires correctly.

---

## Designed flow

```
1. Request enters service
   └─ RegistrationFlowService.executeRegistrationFlow
      - populates ENTITY_ID, ORG, TRUSTMARKS_REQUESTED, TAIM_ID, JOIN_ID
      - if metadata present → also puts REQUEST_METADATA into context

2a. [hosted]     HostedEntityRegistrationStep  (MID, skip if REQUEST_METADATA absent)
    - creates / updates hosted entity via EntityConfigService  → audit fires
    - extracts JWKS from metadata body  → ENTITY_CONFIGURATION_JWKS
    - puts metadata body               → ENTITY_CONFIGURATION_METADATA

2b. [non-hosted] LoadEntityConfigurationStep   (MID, skip if REQUEST_METADATA present)
    - fetches entity configuration from federation endpoint
    - sets ENTITY_CONFIGURATION_JWKS + ENTITY_CONFIGURATION_METADATA (unchanged behaviour)

3.  PublishSubordinateStatementStep             (POST, auto-injected)
    - reads ENTITY_CONFIGURATION_JWKS (set by either 2a or 2b)
    - creates / updates subordinate statement
```

Steps 2a and 2b are mutually exclusive by guard — exactly one runs per request.
Step 3 is path-agnostic; it only cares that ENTITY_CONFIGURATION_JWKS is present.

---

## Changes

### 1. `ContextKey` — add constant

```java
public static final String REQUEST_METADATA = "request_metadata";
```

### 2. `RegistrationFlowService.executeRegistrationFlow` — populate context

```java
// after the existing trustmarks block
final Map<String, Object> bodyMetadata = registrationRequestDto.getMetadata();
if (bodyMetadata != null && !bodyMetadata.isEmpty()) {
    processContext.put(ContextKey.REQUEST_METADATA, bodyMetadata);
}
```

### 3. New step — `HostedEntityRegistrationStep`

Location: `registrationflow/process/step/impl/HostedEntityRegistrationStep.java`

Responsibilities:
- Skip (return success) when `REQUEST_METADATA` is absent — non-hosted path.
- Call `entityConfigService.createHostedEntity` or `updateHostedEntity` — triggers audit.
- Extract JWKS from the metadata body and put it in `ENTITY_CONFIGURATION_JWKS`.
- Put the metadata body in `ENTITY_CONFIGURATION_METADATA`.

The metadata body supplied by the caller is expected to contain the full entity
configuration claims, with the JWKS available at a top-level `jwks` key:

```json
{
  "jwks": { "keys": [ ... ] },
  "openid_provider": { ... },
  "federation_entity": { ... }
}
```

```java
@Component
public class HostedEntityRegistrationStep implements Step {

    private final EntityConfigService entityConfigService;

    public HostedEntityRegistrationStep(EntityConfigService entityConfigService) {
        this.entityConfigService = entityConfigService;
    }

    @Override
    public StepType stepType() { return StepType.MID; }

    @Override
    public boolean isPublic() { return true; }

    @Override
    @Transactional
    public StepResult execute(ProcessContext ctx, StepConfig config) {
        final Optional<Map<String, Object>> bodyMetadata = ctx.get(ContextKey.REQUEST_METADATA);
        if (bodyMetadata.isEmpty()) {
            return StepResult.success("Not a hosted-entity join — step skipped");
        }

        final String entityId        = ctx.getRequired(ContextKey.ENTITY_ID);
        final OrganizationRecord org = ctx.getRequired(ContextKey.ORG);
        final Map<String, Object> metadata = bodyMetadata.get();

        // Persist via service so audit fires
        final List<HostedEntityDto> existing = entityConfigService.listHostedEntity(org, entityId);
        if (existing.isEmpty()) {
            final HostedEntityDto input = new HostedEntityDto();
            input.setEntityIdentifier(entityId);
            input.setMetadata(metadata);
            entityConfigService.createHostedEntity(org, UUID.randomUUID(), input);
        } else {
            final HostedEntityDto current = existing.getFirst();
            current.setMetadata(metadata);
            entityConfigService.updateHostedEntity(org, current.getEntityId(), current);
        }

        // Extract JWKS for downstream steps (expected at top-level "jwks" key)
        final Map<String, Object> jwksMap = (Map<String, Object>) metadata.get("jwks");
        if (jwksMap == null) {
            return StepResult.failure("metadata body missing 'jwks' — cannot create subordinate",
                List.of(new StepIssue("HostedEntityRegistrationStep.jwks",
                    "jwks is required in metadata body", Severity.ERROR)));
        }
        final JWKSet jwkSet = JWKSet.parse(new JSONObject(jwksMap));
        ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, CleanInput.removeExpIatNbfFromJwks(jwkSet));
        ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA, new JSONObject(metadata));

        return StepResult.success("Hosted entity stored and JWKS loaded for: " + entityId);
    }

    @Override
    public UUID getStepId() {
        return UUID.fromString("C3F1A820-5D7B-4E9A-B034-1F6D9A3C7E82");
    }

    @Override
    public List<StepConfigurationValue> getStepConfigurationValues() { return List.of(); }
}
```

### 4. `LoadEntityConfigurationStep` — add hosted guard

Add a single early-exit at the top of `execute`:

```java
@Override
public StepResult execute(ProcessContext ctx, StepConfig config) {
    if (ctx.get(ContextKey.REQUEST_METADATA).isPresent()) {
        return StepResult.success("Hosted-entity join — entity configuration fetch skipped");
    }
    // ... existing implementation unchanged
}
```

### 5. `PublishSubordinateStatementStep` — move to POST

Change `stepType()` from the default `MID` to `POST`:

```java
@Override
public StepType stepType() { return StepType.POST; }
```

`Mapper.toProcessFlow` already auto-injects all POST steps at the end of every
pipeline (via `registrationStepRepository.postDefaultSteps()`). This makes step 3
run automatically without the operator having to include it in the flow definition.
`isPublic()` should return `false` once it is a POST step, since it is no longer
operator-selectable.

### 6. `RegistrationStepRepository` — default MID steps

Add a hardcoded ordered list of step UUIDs and a `defaultMidSteps()` method that
resolves them in order. This follows the same pattern as `preDefaultSteps()` /
`postDefaultSteps()` already in the class — no new interface or annotation needed.

```java
// RegistrationStepRepository.java

private static final List<UUID> DEFAULT_MID_STEP_IDS = List.of(
    UUID.fromString("C3F1A820-5D7B-4E9A-B034-1F6D9A3C7E82"), // HostedEntityRegistrationStep
    UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9")  // LoadEntityConfigurationStep
);

public List<Step> defaultMidSteps() {
    return DEFAULT_MID_STEP_IDS.stream()
        .map(id -> this.findStepById(id)
            .orElseThrow(() -> new IllegalStateException("Default MID step not found: " + id)))
        .toList();
}
```

`Mapper.toModel` and `Mapper.toProcessFlow` substitute defaults when the operator
supplies an empty step list:

```java
// in Mapper.toModel, after building stepModels:
if (stepModels.isEmpty()) {
    stepModels = registrationStepRepository.defaultMidSteps()
        .stream()
        .map(step -> new StepModel(step.getStepId(), List.of()))
        .toList();
}
```

```java
// in Mapper.toProcessFlow, when building MID step definitions:
List<StepModel> flowDef = Optional.ofNullable(dto.getFlowDefinition()).orElse(List.of());
if (flowDef.isEmpty()) {
    flowDef = registrationStepRepository.defaultMidSteps()
        .stream()
        .map(step -> new StepModel(step.getStepId(), List.of()))
        .toList();
}
// then map flowDef to StepDefinitions as before
```

---

## Resulting pipeline for a new flow with default steps

```
PRE   InternalPreRegistrationStep          (auto)
MID   HostedEntityRegistrationStep         (default; no-op for non-hosted)
MID   LoadEntityConfigurationStep          (default; no-op for hosted)
POST  PublishSubordinateStatementStep      (auto)
```

Both MID steps are in every flow by default. Exactly one does real work per request;
the other exits immediately via its guard.

---

## Why `EntityConfigService` and not direct repository access

`EntityConfigServiceImpl.createHostedEntity` / `updateHostedEntity`:
- validates the DTO (`ValidateDto`)
- resolves / creates the `Organization`
- calls `auditService.hostedEntityCreated` / `hostedEntityUpdated`

A direct repository write skips all three. The step must go through the service.

---

## Tests to add

| Test | What to assert |
|---|---|
| Hosted join with metadata + jwks | `createHostedEntity` called; `hostedEntityCreated` in audit; JWKS in context; subordinate created |
| Hosted join, re-submission | `updateHostedEntity` called; `hostedEntityUpdated` in audit |
| Hosted join, metadata missing `jwks` key | Step returns FAILURE; pipeline aborts; no subordinate created |
| Non-hosted join | `HostedEntityRegistrationStep` skips; `LoadEntityConfigurationStep` runs; subordinate created |
| New flow created with empty steps | Persisted flow definition contains the two default MID step IDs |
| `PublishSubordinateStatementStep` | Runs as POST in every pipeline regardless of flow definition |

Integration test: extend `RegistrationFlowEndToEndIT` with both a hosted and a
non-hosted scenario, asserting the correct context keys are set and the correct
audit events fire.
