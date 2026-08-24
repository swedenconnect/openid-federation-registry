![Logo](../docs/images/sweden-connect.png)

# Authorization Model

This document describes how requests to the OpenID Federation Entity Registry API are authenticated and
authorized: the `org_rights` JWT claim, the tenant/function-group matching rules, and which endpoints require
what.

> This replaces the old two-scope (`read`/`write`) model. Access is no longer scope-based — it is derived per
> request from the `org_rights` claim, the organisation number, and the tenant.

## Authentication

Two authentication paths feed the same authorization logic (`infrastructure.config.SecurityConfig`):

- **JWT bearer** (OAuth2 resource server) — for machine/API clients. `Authorization: Bearer <token>`. Parsed by
  `RegistryJwtConverter` into `RegistryClaims`.
- **OIDC login** (OAuth2 client) — for the browser SPA. The session-backed principal is `RegistryOidcUser`.

Both expose the same parsed `org_rights` claim to `OrgRightsService`, so the authorization rules below apply
identically regardless of which path a request came in on.

## The `org_rights` claim

Every authenticated principal carries an `org_rights` claim (JWT claim for bearer tokens, ID token claim for OIDC
sessions), a JSON array with one entry per organisation the caller holds rights on:

```json
"org_rights": [
  {
    "organization_identifier": "5590026042",
    "organization_name#sv": "Litsec AB",
    "organization_name#en": "Litsec AB",
    "functions": [
      { "function": "swedenconnect", "right": "write" }
    ]
  }
]
```

- `organization_identifier` — the organisation number (`orgNumber` in request paths).
- `functions` — one or more `{function, right}` pairs. `function` names a **function group** (a userfunktion); the
  same organisation can hold different rights on different function groups.
- `right` is one of `read`, `write`, `admin` — hierarchical: `admin` implies `write` implies `read`
  (`infrastructure.auth.domain.Right`).

A superuser token skips all of the above:

```json
"org_rights": [{ "superuser": true }]
```

## Tenants and function groups

A **tenant** is a configured federation instance (`openid.federation.registry.instances[]`, see
[Application Configuration](configuration.md#federation-instances)). Each tenant is backed by one or more
**function groups** (`instances[i].function_groups`), and every function group value is unique across all
tenants.

Most endpoints take `{tenant}` and `{orgNumber}` as path variables, e.g.
`GET /registry/v1/{tenant}/{orgNumber}/entities`. Two checks run for such a request:

1. **Org membership** (`infrastructure.auth.OrganizationRecordClaimSelector`) — the tenant slug is resolved to the
   instance's configured function groups; if the tenant is unknown, or the caller's `org_rights` claim has no
   entry for `{orgNumber}`, the request is denied with **403**.
2. **Right level** (`@PreAuthorize("@orgRightsService.canRead/canWrite/canAdmin(...)")`,
   `infrastructure.auth.OrgRightsService`) — the organisation's `functions` entries are matched against the
   tenant's configured function groups; a match is any function name that appears in **both**. The granted right
   is the **highest** right among all matches. If no function group matches, or the matched right doesn't cover
   what the endpoint requires, the request is denied with **403**.

**Example:** tenant `swedenconnect` is configured with `function_groups: [ena, sc, digg]`. A caller whose
`org_rights` holds `{"function": "sc", "right": "read"}` for organisation `44` is granted read access to
`.../swedenconnect/44/...` (`sc` is one of the tenant's function groups). A right on `pm` would **not** grant
access to this tenant — `pm` isn't in its list.

A tenant having more than one function group lets it aggregate organisations arriving under different userfunktion
names into a single administrative tenant.

## Protected endpoints

| API Path                                   | Authentication required | Right-level enforcement                                                                                    |
|--------------------------------------------|:-----------------------:|------------------------------------------------------------------------------------------------------------|
| `/registry/v1/**`                          |           Yes           | Per-method `@PreAuthorize` (`canRead`/`canWrite`/`canAdmin`)                                               |
| `/registration-flow/v1/**`                 |           Yes           | Per-method `@PreAuthorize`                                                                                 |
| `/registration-admin/v1/**`                |           Yes           | Per-method `@PreAuthorize`                                                                                 |
| `/registration/v1/{tenant}/{orgNumber}/**` |           Yes           | Per-method `@PreAuthorize` (`canRead`/`canWrite`)                                                          |
| `/registration/v1/flows`                   |           Yes           | None beyond authentication — browsing available registration flows doesn't require belonging to an org yet |
| `/tenants`, `/userinfo`                    |           Yes           | Resolved from the caller's own `org_rights`, no path-variable tenant/org                                   |
| `/swagger-ui/**`, `/v3/api-docs/**`        |           Yes           | —                                                                                                          |

### Public endpoints (no authentication required)

| Path pattern                                                      | Notes                                                           |
|-------------------------------------------------------------------|-----------------------------------------------------------------|
| `GET /api/v1/federationservice/**`                                | Public API the external oidf-service federation node reads from |
| `GET /actuator/**`                                                | Health / metrics endpoints                                      |
| `GET /assets/**`                                                  | Static frontend assets                                          |
| `GET /entities/**`, `/registration-flows/**`, `/registrations/**` | Public read-only UI pages                                       |
| `GET /logout/frontchannel`                                        | OIDC front-channel logout                                       |

See [Architecture Overview](architecture.md#multi-tenancy-and-authorization-model) for how this fits into the
rest of the system, and `infrastructure.config.SecurityConfig` for the authoritative, up-to-date rule set.

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
