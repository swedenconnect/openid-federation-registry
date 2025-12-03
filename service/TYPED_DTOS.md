# Typade DTO:er för OIDF Entity Registry

## Översikt

Detta dokument beskriver de typade DTO:erna (Data Transfer Objects) som har implementerats för att förbättra type safety
och validering i OIDF Entity Registry.

## Problem som löses

### Tidigare implementation

- Alla CRUD-operationer använde den generiska `OptionsRecord`-klassen
- Ingen type safety för specifika entitetstyper
- Svår validering av entitetsspecifika fält
- Kompilatorn kunde inte fånga fel vid felaktig användning

### Ny implementation

- Typade DTO:er för varje entitetstyp (Policy, TrustMark, etc.)
- Kompilatorn fångar fel vid felaktig användning
- Entitetsspecifik validering
- Bättre IDE-stöd med autocomplete

## Implementerade DTO:er

### 1. PolicyOptionsRecord

**Plats:** `se.swedenconnect.oidf.registry.api.dto.PolicyOptionsRecord`

**Specifika fält:**

- `policyId` (UUID)
- `policyName` (String)
- `policyDescription` (String)
- `policyUri` (String)
- `policyDigest` (String)

**Validering:**

- Policy name är obligatorisk
- Policy URI valideras som giltig URI-format
- UUID-fält valideras som giltiga UUID:er

### 2. TrustMarkOptionsRecord

**Plats:** `se.swedenconnect.oidf.registry.api.dto.TrustMarkOptionsRecord`

**Specifika fält:**

- `trustmarkIssuerId` (UUID)
- `trustmarkId` (UUID)

## Användning

### 1. Via Typed Controller

```java
// Skapa en ny policy
PolicyOptionsRecord policy = PolicyOptionsRecord.builder()
    .option(List.of(
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_name")
            .policyName("Min Policy")
            .valueType("TEXT")
            .validation("required")
            .settingDescription("Policy name")
            .build(),
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_uri")
            .policyUri("https://example.com/policy")
            .valueType("URL")
            .validation("URL")
            .settingDescription("Policy URI")
            .build()
    ))
    .build();

// Använd den typade controllern
POST /registry/v1/typed-options/policies/{id}
```

### 2. Via Service Layer

```java
// Använd den typade service-metoden
PolicyOptionsRecord created = optionsCRUDPolicy.createTyped(
    organizationRecord, 
    FkKeyType.POLICIES, 
    policyId, 
    policyRecord
);
```

### 3. Konvertering mellan generisk och typad

```java
// Från generisk till typad
PolicyOptionsRecord typed = PolicyOptionsRecord.fromOptionsRecord(genericRecord);

// Från typad till generisk
OptionsRecord generic = typedRecord.toOptionsRecord();
```

## API Endpoints

### Typade Endpoints

- `POST /registry/v1/typed-options/policies/{id}` - Skapa policy
- `PUT /registry/v1/typed-options/policies/{id}` - Uppdatera policy
- `GET /registry/v1/typed-options/policies/{id}` - Hämta policy
- `DELETE /registry/v1/typed-options/policies/{id}` - Ta bort policy

### Bakåtkompatibilitet

De ursprungliga endpoints finns kvar för bakåtkompatibilitet:

- `POST /registry/v1/options/policies/{id}`
- `PUT /registry/v1/options/policies/{id}`
- `GET /registry/v1/options/policies/{id}`
- `DELETE /registry/v1/options/policies/{id}`

## Fördelar

### 1. Type Safety

```java
// Detta kommer att ge kompilatorfel
PolicyOptionsRecord.PolicyValues.builder()
    .policyName("Test")  // ✅ Korrekt
    .invalidField("Test") // ❌ Kompilatorfel
    .build();
```

### 2. Validering

```java
// Automatisk validering av policy-specifika fält
PolicyOptionsRecord record = PolicyOptionsRecord.builder()
    .option(List.of(
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_uri")
            .policyUri("invalid-uri") // ❌ Validering fel
            .build()
    ))
    .build();
```

### 3. IDE-stöd

- Autocomplete för specifika fält
- Refactoring-stöd
- Bättre dokumentation

## Framtida utveckling

### Planerade DTO:er

- `EntityOptionsRecord` - För entiteter
- `ModuleOptionsRecord` - För moduler
- `TrustMarkSubjectOptionsRecord` - För trust mark subjects

### Förbättringar

- Automatisk generering av DTO:er från OpenAPI-specifikationen
- Mer omfattande validering med Bean Validation
- GraphQL-stöd för typade queries

## Migration

### Från generisk till typad

1. **Identifiera entitetstyp**
   ```java
   // Tidigare
   OptionsRecord record = optionsCRUD.get(org, FkKeyType.POLICIES, id);
   
   // Nytt
   PolicyOptionsRecord record = optionsCRUDPolicy.getTyped(org, FkKeyType.POLICIES, id);
   ```

2. **Använd typade fält**
   ```java
   // Tidigare
   String policyName = record.getOption().stream()
       .filter(v -> "policy_name".equals(v.getKey()))
       .findFirst()
       .map(Values::getValue)
       .orElse(null);
   
   // Nytt
   String policyName = record.getOption().stream()
       .filter(v -> "policy_name".equals(v.getKey()))
       .findFirst()
       .map(PolicyOptionsRecord.PolicyValues::getPolicyName)
       .orElse(null);
   ```

3. **Använd typade endpoints**
   ```java
   // Tidigare
   POST /registry/v1/options/policies/{id}
   
   // Nytt
   POST /registry/v1/typed-options/policies/{id}
   ```

## Exempel

### Komplett Policy-skapande

```java
PolicyOptionsRecord policy = PolicyOptionsRecord.builder()
    .option(List.of(
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_name")
            .policyName("GDPR Compliance Policy")
            .valueType("TEXT")
            .validation("required")
            .settingDescription("Name of the policy")
            .build(),
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_description")
            .policyDescription("Policy for GDPR compliance")
            .valueType("TEXT")
            .validation("optional")
            .settingDescription("Description of the policy")
            .build(),
        PolicyOptionsRecord.PolicyValues.builder()
            .key("policy_uri")
            .policyUri("https://example.com/gdpr-policy")
            .valueType("URL")
            .validation("URL")
            .settingDescription("URI to the policy document")
            .build()
    ))
    .build();

// Skapa via service
PolicyOptionsRecord created = optionsCRUDPolicy.createTyped(
    organizationRecord, 
    FkKeyType.POLICIES, 
    UUID.randomUUID(), 
    policy
);
```

Detta ger dig full type safety och validering för policy-operationer!

