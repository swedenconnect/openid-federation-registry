![Sweden Connect](images/sweden-connect.png)

# OIDF Registry - Release Notes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### Version 0.9.8 Unreleased
**Date:** <ToBeSet>

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
- The trust mark issuers of a trust anchor are now exported to oidf-service as `trust_mark_issuers`. **Breaking change in
  the API:** `trustMarkIssuers` of a trust anchor is a list of entries `{issuer, auto, trustMarkTypes}` instead of a list
  of entity identifiers. With `auto` set, every trust mark of the issuer is included and the trust mark types are read
  from the issuer when oidf-service fetches its configuration, so new and removed trust marks are followed without
  changing the trust anchor. The issuer has to be a trust mark issuer of the same organization, otherwise the request
  is rejected with 400, and an issuer that has been removed or deactivated since is left out with a warning in the log.
  Without `auto`, `trustMarkTypes` lists the trust mark types and the issuer can be any entity. Database migration V32
  moves the issuers that are stored today to entries with `auto` set. The trust anchor panel in the GUI has a row per
  issuer with the auto option, suggestions, a status for each row and the list of trust mark types that are trusted.

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
