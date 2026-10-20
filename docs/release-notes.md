![Sweden Connect](images/sweden-connect.png)

# OIDF Registry - Release Notes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### Version 0.9.10 Unreleased
**Date:** <ToBeSet>

- CSRF protection for requests from the GUI. Logout is a `POST`, and the session cookie is sent with `SameSite=Lax`.
- The registry does not start when `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` is set without
  `spring.security.oauth2.resourceserver.jwt.issuer-uri`, since the `iss` claim of access tokens is then not checked.
- Only the actuator endpoints `health` and `info` are exposed and open without authentication.
- The session cookie is marked `Secure` also behind the TLS proxy (#322).
- Content-Security-Policy, Referrer-Policy and Permissions-Policy headers. The icon font is no longer loaded from a CDN (#324).
- API: `Registration` has `pinnedMetadata` for a self-managed entity: the parameters and keys its subordinate statement
  pins.
- Resubmitting an approved self-managed entity passes its review steps when no pinned value or key changes. When one
  changes, it waits for review and the federation keeps the published values until it is approved.
- API: a resubmission of a self-managed entity that both adds and removes keys is refused with `400` (key rollover:
  add the new key first, remove the old one in a later resubmission).
- Flow steps that can pause for review have `manualreviewforupdate`: the step also reviews a resubmission that changes
  no pinned value or key. API: `pinnedMetadata.reviewOnUpdate` is set when a step of the flow has it. GUI: a checkbox
  in the flow builder.
- API: an entity review has `update`, set when the entity is already in the federation. GUI: an **Update** chip on
  the entity, and the Metadata Review step shows the changes to the values published in the federation.

### Version 0.9.9 Unreleased
**Date:** <ToBeSet>

- The `ec_location` that is calculated for a hosted entity now has a `hosted` path segment, for example `https://registry.example.se/oidf/hosted/www_example_org`
  instead of `https://registry.example.se/oidf/www_example_org`. **The location changes for existing hosted entities**
  and for subordinates that resolve it automatically. The segment is set with
  `openid.federation.registry.hosted-path-segment`, and an empty value leaves it out.
- The trust mark issuers of a trust anchor are now exported to oidf-service as `trust_mark_issuers`. **Breaking change in
  the API:** `trustMarkIssuers` of a trust anchor is a list of entries `{issuer, auto, trustMarkTypes}` instead of a list
  of entity identifiers. With `auto` set, every trust mark of the issuer is included and the trust mark types are read
  from the issuer when oidf-service fetches its configuration, so new and removed trust marks are followed without
  changing the trust anchor. The issuer has to be a trust mark issuer of the same organization, otherwise the request
  is rejected with 400, and an issuer that has been removed or deactivated since is left out with a warning in the log.
  Without `auto`, `trustMarkTypes` lists the trust mark types and the issuer can be any entity. Database migration V32
  moves the issuers that are stored today to entries with `auto` set. The trust anchor panel in the GUI has a row per
  issuer with the auto option, suggestions, a status for each row and the list of trust mark types that are trusted.
- A trust anchor can also export trust mark types with issuers outside the organization, in `externalTrustMarks`
  `{trustMarkType, allowAll, issuers}`. With `allowAll` anyone may issue the trust mark type, and it is exported with an
  empty list of issuers. The trust anchor panel in the GUI has a section for them.
- Exporting of metrics and traces is now off by default: `management.prometheus.metrics.export.enabled`,
  `management.otlp.metrics.export.enabled` and `management.tracing.export.enabled` are `false` in `application.yml`.
  Set them to `true` to turn the export on.
- A failed login, for example when the tokens can not be exchanged, now sends the user to the login page with a message.
  The error code from Spring Security or the identity provider, such as `invalid_token_response`, is shown as the reason. A login that is denied or cancelled has a message of its own. The redirect keeps the context path, so it also works
  when the registry is not served from the root.
- New entity review API under `/registration-admin/v1/{tenant}/{orgNumber}/entity-reviews`: list, get, `start` and
  `decisions`. Open to tenant operators (`operator_organizations`).
- Removed `GET /registration-admin/v1/{tenant}/{orgNumber}/count`.
- `Registration` has new fields `createdDate`, `reviewedBy` and `reviewedAt`. `StepExecutionRecord` has new fields
  `preview`, `reviewedBy` and `reviewedAt`.
- New audit events `REGISTRATION_STEP_APPROVED` and `REGISTRATION_REJECTED`.
- GUI: registrations are reviewed per entity in a shared session, with a preview of each paused step.
- New step **Metadata Review**.
- **Metadata Policy Creation** uses a configurable template per entity type. Subordinate statements publish `metadata`.
- Fixed: approving a paused registration lost its keys and metadata policy.
- A registration or trust mark request whose automated step fails is now declined automatically (`REJECTED`, with the
  failed step as `rejectionReason`) instead of staying pending.
- A requested trust mark with no usable registration flow is no longer skipped and left pending. It is `APPROVED` if
  the entity already holds the trust mark, otherwise `REJECTED` with the reason.
- GUI and API: a flow step can be shown for the user (`showforuser`), with a display name and description in Swedish
  and English that are then required. The step results of a registration carry them as `displayName`,
  `displayNameSv`, `displayDescription` and `displayDescriptionSv`.
- Templates can be configured under `openid.federation.registry.templates` (`step` or `policy`) and are listed by
  `GET /registry/v1/{tenant}/{orgNumber}/templates`. GUI: **Load template** in the Metadata Policy Creation step.
- API: Trust Anchors and Intermediates have `metadataPolicy`, combined with each subordinate's own policy in every
  subordinate statement. A policy that cannot be combined is refused with `400`. GUI: **Metadata policy for
  subordinates** with **Load template**.
- API: a trust mark in `statusTrustmarks` has `rejectionReason` when it is `REJECTED`.
- A flow step can set `userdeclinereason`, the reason the member sees when the step fails. Automatic decline reasons
  have no technical details.
- API: the registration admin API has `technicalReason` for an automatic decline. GUI: **Technical Reason** on the
  registration.
- API: step issues have `type`, `VALIDATION` or `TECHNICAL`. GUI: the issue type on the registration.
- A registration whose entity configuration cannot be fetched or is not valid is declined, instead of the request
  failing.
- API: only a hosted entity's identifier has to be on a registered domain of the organization; a self-managed entity
  needs no domain claim.
- GUI: list views have previous/next pagination, and subordinates can be searched, sorted and paged (#310).
- GUI: federation entities show icons for their active modules and no Resolver button (#310).
- GUI: the edit page of a subordinate has a button that fetches and shows its subordinate statement, and the entity
  configuration check and button are also on the create page.
- GUI: a trustmark subject is opened by clicking its row, and is deleted from its edit view (#310).
- API: a trust mark in `statusTrustmarks` has `steps`, its own step results; also in the response to a registration
  request.
- API: `PUT /registry/v1/{tenant}/{orgNumber}/subordinates/metadata-policy` sets a policy on all subordinates of a
  Trust Anchor or Intermediate. GUI: after loading a template on a Trust Anchor or Intermediate, an offer to apply it
  to all its subordinates.
- GUI: **Load template** on a subordinate's metadata policy.


---

### Version 0.9.8
**Date:** 2026-10-09

- `ec_location` is no longer added to `crit` for hosted entities whose entity identifier lies outside the registry's
  entity prefix. The location is still calculated and used to fetch the entity configuration.
- The Entity menu item is replaced by **Federation** (operators) and **Hosted Entities**. Federation entities are
  shown as cards with a link to the entity configuration, an edit button and one button per role (Subordinates,
  Trustmarks, Resolver). The hosted entities are shown as a sortable list with a search field on the entity
  identifier. Deleting an entity, a trustmark or a subordinate is done from its edit page.
- Federation entities have an optional `name`, set in the create and edit forms and exposed in the API. The entity
  identifier is shown when no name is set. Database migration V31 adds the column.
- Fixed `GET .../entities?type=federation|hosted`, which rejected the documented values with a 400 error.
- In the trustmark list a click on a row opens its subjects and a cog icon opens the edit page. In the subordinate list a
  click on a row opens the edit page.
- Creating a subordinate checks automatically that the entity configuration of the entered entity can be loaded, and
  shows a green check or a red cross.
- Creating a trustmark subject suggests the hosted entities of the organization.
- The trustmark sources of a hosted entity suggest the trustmark issuers of the organization and their trustmarks,
  and mark whether the trustmark is assigned to the entity.
- `DELETE .../entities/hosted/{entityId}` takes an optional `deleteTrustmarkSubjects` flag (default `false`). When set,
  the entity is also removed as subject of the trustmarks pointed out by its trustmark sources, as far as those
  trustmarks belong to the same organization.
- The no-access page now explains that the account has no function group supported by the registry, and offers a
  logout button.
- The frontend shows an error with retry and logout when the user's permissions cannot be loaded, instead of an empty
  page.
- A superuser in a tenant without registered organizations now sees an explanatory message, and the selected tenant is
  kept across page loads.
- The frontend is now built by the frontend-maven-plugin as part of the Maven build and copied into the static resources
  of the service. The built frontend files are no longer checked into git, and the GitHub Actions workflows no longer
  build the frontend by hand. Use `-Dfrontend.skip=true` to leave the frontend out of a build.

---

### Version 0.9.7

**Date:** 2026-10-07

- The `/submodules` response no longer contains null values or empty objects and arrays.

---

### Version 0.9.6

**Date:** 2026-10-06

- Fixed Flyway failing with a checksum mismatch on V28 when a second node or a restart used a database created by a
  fresh install.
- Subordinate statements can now carry constraints.
- The validity duration of trust mark issuers is now optional.
- The frontend shows a clear message for 403 responses and a no-access page.
- The release script now tags on the release branch and sets the next development version there.

---

Copyright &copy; 2026, [Myndigheten för digital förvaltning - Swedish Agency for
Digital Government (DIGG)](https://www.digg.se). Licensed under version 2.0 of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0).
