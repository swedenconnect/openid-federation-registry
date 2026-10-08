![Sweden Connect](images/sweden-connect.png)

# OIDF Registry - Release Notes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### Version 0.9.8 Unreleased
**Date:** <ToBeSet>

- The no-access page now explains that the account has no function group supported by the registry, and offers a
  logout button.
- The frontend shows an error with retry and logout when the user's permissions cannot be loaded, instead of an empty
  page.
- A superuser in a tenant without registered organizations now sees an explanatory message, and the selected tenant is
  kept across page loads.

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
