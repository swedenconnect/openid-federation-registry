# OpenAPI / Swagger Documentation

Detta projekt använder **springdoc-openapi** för att automatiskt generera OpenAPI-dokumentation från Spring Boot
controllers och DTO-klasser.

## Komma åt dokumentationen

När applikationen körs kan du komma åt OpenAPI-dokumentationen på följande sätt:

### 1. Swagger UI (Interaktiv dokumentation)

Öppna din webbläsare och gå till:

```
http://localhost:8010/swagger-ui.html
```

Eller om du använder en annan port, ersätt `8010` med din server-port.

**Swagger UI** ger dig:

- En interaktiv webbgränssnitt där du kan utforska alla API-endpoints
- Möjlighet att testa API-anrop direkt från webbläsaren
- Automatisk dokumentation av alla request/response-modeller
- Exempel på hur API-anrop ska se ut

### 2. OpenAPI JSON (Machine-readable)

För att få OpenAPI-specifikationen som JSON:

```
http://localhost:8010/v3/api-docs
```

Detta returnerar en komplett OpenAPI 3.0-specifikation i JSON-format som kan användas med:

- OpenAPI-generator för att skapa klienter
- Postman för att importera API-kollektioner
- Andra API-dokumentationsverktyg

### 3. OpenAPI YAML

För YAML-format:

```
http://localhost:8010/v3/api-docs.yaml
```

## Konfiguration

OpenAPI-konfigurationen finns i:

- **Java-konfiguration**: `se.swedenconnect.oidf.registry.config.OpenApiConfig`
- **YAML-konfiguration**: `application.yml` under `springdoc`

### Anpassa dokumentationen

För att ändra API-information (titel, beskrivning, version, etc.), redigera `OpenApiConfig.java`.

För att ändra Swagger UI-inställningar (sökvägar, sortering, etc.), redigera `application.yml` under `springdoc`.

## Autentisering

API:et använder JWT Bearer-token för autentisering. I Swagger UI kan du:

1. Klicka på **"Authorize"**-knappen (lås-ikonen) i övre högra hörnet
2. Ange din JWT-token i formatet: `Bearer <din-token>`
3. Klicka på **"Authorize"** och sedan **"Close"**

Nu kommer alla API-anrop från Swagger UI att inkludera din token i Authorization-headern.

## API-gruppering

Endpoints är grupperade i följande kategorier (tags):

- **Entities** - Hantering av Federation/Hosted/Subordinate entities
- **Policies** - Hantering av metadata policies
- **Modules** - Hantering av federation-moduler (TrustAnchor, Resolver, Trustmark)
- **Trustmark Subjects** - Hantering av trust mark subjects
- **Options** - Legacy options API (behålls för bakåtkompatibilitet)

## Exempel på användning

### Testa ett API-anrop från Swagger UI

1. Öppna `http://localhost:8010/swagger-ui.html`
2. Expandera en endpoint (t.ex. `POST /registry/v1/policies/{id}`)
3. Klicka på **"Try it out"**
4. Fyll i parametrar och request body
5. Klicka på **"Execute"**
6. Se response nedan

### Importera till Postman

1. Öppna Postman
2. Klicka på **Import**
3. Välj **Link** och ange: `http://localhost:8010/v3/api-docs`
4. Postman kommer automatiskt att skapa en kollektion med alla endpoints

### Generera klient-kod

Med OpenAPI-generator kan du generera klient-kod för olika språk:

```bash
# Exempel: Generera Java-klient
openapi-generator-cli generate \
  -i http://localhost:8010/v3/api-docs \
  -g java \
  -o ./generated-client
```

## Felsökning

### Swagger UI visas inte

- Kontrollera att `springdoc.swagger-ui.enabled=true` i `application.yml`
- Kontrollera att applikationen körs på rätt port
- Kontrollera att inga säkerhetsregler blockerar `/swagger-ui.html` eller `/v3/api-docs`

### Endpoints visas inte

- Kontrollera att controllers har `@RestController` eller `@Controller` annotation
- Kontrollera att endpoints har `@Operation` eller `@Tag` annotations för bättre dokumentation
- Kontrollera att DTO-klasser har `@Schema` annotations för fältdokumentation

### Autentisering fungerar inte

- Kontrollera att SecurityConfig tillåter `/swagger-ui.html` och `/v3/api-docs`
- Kontrollera att JWT-token är giltig och inte har gått ut
- Kontrollera att token-formatet är korrekt: `Bearer <token>`

