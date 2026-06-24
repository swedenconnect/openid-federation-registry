![Logo](../docs/images/sweden-connect.png)

# OAuth2 Scopes

This document describes the OAuth2 scopes required to access the OpenID Federation Entity Registry API.

## Overview

The Entity Registry Service uses OAuth2 JWT tokens for authentication and authorization. All protected endpoints
use exactly two scopes: `read` and `write`. There are no per-resource scopes.

## Scope Definitions

| Scope   | HTTP Methods            | Description                                  |
|---------|-------------------------|----------------------------------------------|
| `read`  | `GET`                   | Read access to all protected API resources.  |
| `write` | `POST`, `PUT`, `DELETE` | Write access to all protected API resources. |

## Protected Endpoints

The table below lists which API path groups require which scope:

| API Path                              | `read` (GET) | `write` (POST / PUT / DELETE) |
|---------------------------------------|:------------:|:-----------------------------:|
| `/registry/v1/entities/hosted/**`     |      ✓       |               ✓               |
| `/registry/v1/entities/**`            |      ✓       |               —               |
| `/registry/v1/entities/federation/**` |      ✓       |               ✓               |
| `/registry/v1/modules/**`             |      ✓       |               ✓               |
| `/registry/v1/trustmarks/**`          |      ✓       |               ✓               |
| `/registry/v1/trustmarks/subjects/**` |      ✓       |               ✓               |
| `/registry/v1/subordinates/**`        |      ✓       |               ✓               |
| `/registry/v1/entityconfiguration/**` |      ✓       |               ✓               |
| `/registration-flow/v1/**`            |      ✓       |               ✓               |
| `/registration/v1/**`                 |      ✓       |               ✓               |
| `/registration-admin/v1/**`           |      ✓       |               ✓               |

### Public endpoints (no scope required)

| Path pattern                                                  | Notes                         |
|---------------------------------------------------------------|-------------------------------|
| `/api/v1/federationservice/**`                                | Public federation service API |
| `/actuator/**`                                                | Health / metrics endpoints    |
| `/assets/**`                                                  | Static frontend assets        |
| `/entities/**`, `/registration-flows/**`, `/registrations/**` | Public read-only UI pages     |
| `/logout/frontchannel`                                        | OIDC front-channel logout     |

## Usage

Include a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

The JWT token must contain the required scope in the `scope` claim.

### Read-only token example

```
scope: "read"
```

### Full-access token example

```
scope: "read write"
```

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).