![Logo](../docs/images/sweden-connect.png)

# OAuth2 Scopes

This document describes the OAuth2 scopes required to access different endpoints of the OpenID Federation Entity
Registry API.

## Overview

The Entity Registry Service uses OAuth2 JWT tokens for API authentication and authorization. Each endpoint requires
specific scopes to be present in the JWT token. Scopes are organized by resource type and operation (read or write).

## Scope Definitions

The following table lists all available OAuth2 scopes and their corresponding API paths and HTTP methods:

| Scope Name                                                 | API Path                              | HTTP Methods            | Description                                           |
|------------------------------------------------------------|---------------------------------------|-------------------------|-------------------------------------------------------|
| `http://registry.swedenconnect.se/entity/hosted/read`      | `/registry/v1/entities/hosted/**`     | `GET`                   | Read access to hosted entity                          |
| `http://registry.swedenconnect.se/entity/hosted/write`     | `/registry/v1/entities/hosted/**`     | `POST`, `PUT`, `DELETE` | Write access to hosted entity                         |
| `http://registry.swedenconnect.se/modules/read`            | `/registry/v1/modules/**`             | `GET`                   | Read access to module resources and federation entity |
| `http://registry.swedenconnect.se/modules/write`           | `/registry/v1/modules/**`             | `POST`, `PUT`, `DELETE` | Write access to module resources                      |
| `http://registry.swedenconnect.se/trustmarks/read`         | `/registry/v1/trustmarks/**`          | `GET`                   | Read access to trust mark resources                   |
| `http://registry.swedenconnect.se/trustmarks/write`        | `/registry/v1/trustmarks/**`          | `POST`, `PUT`, `DELETE` | Write access to trust mark resources                  |
| `http://registry.swedenconnect.se/trustmarksubjects/read`  | `/registry/v1/trustmarks/subjects/**` | `GET`                   | Read access to trust mark subject resources           |
| `http://registry.swedenconnect.se/trustmarksubjects/write` | `/registry/v1/trustmarks/subjects/**` | `POST`, `PUT`, `DELETE` | Write access to trust mark subject resources          |
| `http://registry.swedenconnect.se/policies/read`           | `/registry/v1/policies/**`            | `GET`                   | Read access to policy resources                       |
| `http://registry.swedenconnect.se/policies/write`          | `/registry/v1/policies/**`            | `POST`, `PUT`, `DELETE` | Write access to policy resources                      |
| `http://registry.swedenconnect.se/subordinates/read`       | `/registry/v1/subordinates/**`        | `GET`                   | Read access to subordinate entity resources           |
| `http://registry.swedenconnect.se/subordinates/write`      | `/registry/v1/subordinates/**`        | `POST`, `PUT`, `DELETE` | Write access to subordinate entity resources          |

## Usage

When making API requests, include a valid JWT token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

The JWT token must contain the required scope(s) in the `scope` claim. Multiple scopes can be included in a single
token, separated by spaces.

### Example

To read entities, your JWT token should include:

```
scope: "http://registry.swedenconnect.se/entity/read"
```

To both read and write entities, your JWT token should include:

```
scope: "http://registry.swedenconnect.se/entity/read http://registry.swedenconnect.se/entity/write"
```

## Resource Types

- **Entities**: Federation entity configurations and metadata
- **Modules**: Federation module definitions
- **Trust Marks**: Trust mark definitions and configurations
- **Trust Mark Subjects**: Assignments of trust marks to subjects
- **Policies**: Federation policies that govern entity behavior
- **Subordinates**: Subordinate entity relationships and configurations

---

Copyright &copy; 2025, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).