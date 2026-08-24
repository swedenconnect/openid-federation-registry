![Sweden Connect](images/sweden-connect.png)

# OpenID Federation Registry

A graphical administration interface for an OpenID Federation service node. The registry allows
administrators to register and manage federation entities — organisations that participate in the
federation are onboarded here, and their entity configurations are validated and published as
subordinate entity statements by the underlying federation service.

## Documentation

### Architecture

- [Architecture Overview](architecture.md) — How the system fits together: components, the
  backend package map, the multi-tenancy and authorization model, and the data model.

- [Registration Flow](use_case_registration_flow.md) — The step-by-step flow for how an
  organisation's entity joins the federation via an Intermediate: from entity configuration
  loading through validation to subordinate statement publication.

### Integration

- [Authorization Model](oauth.md) — The `org_rights` JWT claim, tenant/function-group matching,
  and which endpoints require what.

### Administration

- [Application Configuration](configuration.md) — Full configuration reference: server, database,
  security, credential bundles, federation instances, and entity configuration loader settings.

### Operations

- [Developer Guide](developer.md) — How to set up a local development environment, build the
  project, and run integration tests.

- [Audit Events](audit.md) — Audit events emitted by the registry service: what triggers them,
  their structure, and how to configure audit log output.

- [Validation](validation.md) — The property validation rule DSL used across DTOs.

- [Frontend Spec](frontend_spec.md) — SPA technology choices and UI conventions.

---

Copyright &copy; 2026, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
