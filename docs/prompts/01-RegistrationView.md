Build a new "Incoming registrations" view in the oidf-entity-registry GUI.

## Context
This project administers an OpenID Federation (OIDF spec:
https://openid.net/specs/openid-federation-1_0.html).
The registry manages trust anchors and intermediates. Entities
submit explicit registration requests to a specific intermediate.
Each request is scoped to one intermediate, identified by its
internal UUID (taimId).

## Goal
Add a dedicated view that lists all incoming PENDING registration
requests directed at intermediates managed by this organisation.
Use the existing registration-admin REST API.

## Route
/admin/registrations

Add a navigation entry "Incoming registrations" in the sidebar
with a live badge showing the count of pending requests.

## Data model
Each registration request record returned by the API includes:

id                  UUID    — registration ID (read-only)
taimId              UUID    — UUID of the intermediate (TaIm)
                             the request is addressed to
registrationFlowId  UUID    — registration flow used
entityId            string  — entity ID (URL) of the applicant
jwks                string  — JWKS snapshot (JSON string)
metadata            string  — entity configuration metadata snapshot
metadataPolicy      string  — proposed metadata policy (JSON string)
trustmarksRequested string  — requested trustmarks
status              string  — PENDING | APPROVED | REJECTED
reviewedAt          string  — ISO 8601 datetime, present after review
reviewedBy          string  — identity of reviewing operator
rejectionReason     string  — present when status is REJECTED
createdDate         string  — ISO 8601 datetime of submission

Note: org_name, entity_type, and policy_result fields do NOT exist
in the API. The list endpoint always returns PENDING records only —
there is no status filter. There is no pagination.

## API endpoints to use

GET  /registration-admin/v1/?taimId=<UUID>
Required query param: taimId (UUID of the intermediate).
Returns a flat list of PENDING registration request records for
that intermediate. No pagination, no other filters.

GET  /registration-admin/v1/count?taimId=<UUID>
Required query param: taimId (UUID of the intermediate).
Returns: { "count": <number> }
Use this to populate the sidebar badge and the "Pending" stat card.

POST /registration-admin/v1/{id}/reject
Body: { "rejectionReason": "<string>" }
Rejects a PENDING request. Returns the updated record.

NOTE — there is NO approve endpoint. Approval is handled by opening
the subordinate dialog (pre-filled from the registration record),
not by a direct POST to this API.

NOTE — there is NO endpoint for fetching a single record by ID,
no trust-chain endpoint, and no status/type/search query params.

## View requirements

1. Summary stat card (top of page)
   Show one metric tile: Pending review count for the selected
   intermediate. Poll every 30 seconds and update the sidebar
   badge accordingly.

2. Toolbar
    - Intermediate selector — required; lists all intermediates
      managed by this organisation. Each option shows the
      intermediate entity ID (URL). Selecting one loads pending
      requests for that intermediate's taimId. No intermediate
      selected → show prompt to select one.
    - Refresh button

3. Request table
   Columns (fixed layout, no horizontal scroll):
   Entity ID       — entityId (monospace, truncated with tooltip)
   Received        — relative time (e.g. "2h ago") from createdDate
   Status          — status pill (always PENDING in this view)
   Actions         — "Review" button opens the subordinate dialog
                     pre-filled with this registration's data;
                     "Reject" button opens a confirmation dialog
                     that calls POST /{id}/reject

   Clicking a row (outside the action buttons) has no navigation
   target — there is no detail page endpoint. Show a read-only
   detail panel or modal if needed, using the data already loaded.
   Rows are sortable by Received column.
   No pagination (display all returned records).

4. Empty state
   When no requests are returned, show a centred empty state with
   an icon and "No pending requests for this intermediate" message.

## Status badge colours
PENDING  → warning semantic colour
APPROVED → success semantic colour
REJECTED → danger semantic colour

## Technical notes
- Follow the existing project conventions for routing, API
  client (useRequest composable), component structure, and styling.
- taimId is a UUID, not a URL. Map the selected intermediate's
  entity ID (URL) to its taimId before calling the API. Look at
  how existing views load the list of managed intermediates to
  find the correct endpoint and the taimId field on each record.
- Use the existing loading/error state patterns from other
  list views in the project.
- Do not hardcode any entity IDs or organisation names —
  all data comes from the API.