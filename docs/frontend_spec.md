# OIDF Registry Admin

## Technology Choices

### Component Library

The application uses **Vuetify 3** as the primary component library. All UI components should, as far as possible, use
Vuetify components to ensure consistency, accessibility, and maintainability.

**Vuetify Components Used**:

- `v-app`, `v-app-bar`, `v-main` for layout
- `v-card`, `v-card-title`, `v-card-text`, `v-card-actions` for content containers
- `v-btn` for buttons
- `v-table`, `v-table-row`, `v-table-cell` for tables
- `v-form`, `v-text-field`, `v-textarea`, `v-select`, `v-combobox`, `v-switch` for forms
- `v-dialog` for modals and dialogs
- `v-alert` for error messages and notifications
- `v-progress-circular` for loading indicators
- `v-expansion-panels`, `v-expansion-panel` for collapsible sections
- `v-chip` for status indicators
- `v-spacer` for layout spacing
- `v-container`, `v-row`, `v-col` for responsive grid layout

**Icon Library**: Material Design Icons (MDI) via `@mdi/js` for SVG icons.

Color Tokens

- --ink: #1a1a1a (primary text, near-black)
- --muted: #5c5c5c (secondary text, hints)
- --brand-sage: #4a6741 (primary brand, nav active states, primary buttons)
- --brand-sage-light: #f0f4ee (sage tint for surfaces, backgrounds)
- --brand-rose: #b86a5e (accent, highlights, badges)
- --brand-rose-light: #f9ecea (rose tint surface)
- --surface: #ffffff
- --surface-alt: #f5f5f3 (warm off-white for subtle sections)
- --border: #d6d6d2 (neutral warm-gray border)
- --sweden-blue: #005b99 (Swedish federation/trust context accents)
- --sweden-yellow: #f0c30f (Swedish federation accents)

Status palette
- Green (approved/complete): bg #e8f2e6, text #2d6b28, border #b3d4ae
- Amber (pending/warning): bg #fdf3de, text #7a4f00, border #e8c96a
- Red (error/rejected): bg #fdecea, text #8c2020, border #e8b4b0
- Gray (locked/disabled): bg #f0f0ee, text #666663, border #d0d0cc
### Form Validation and API Integration

All forms must follow the **OpenAPI specification** from the backend API. The backend provides an OpenAPI
specification (available at `/v3/api-docs` or `/v3/api-docs.yaml`) that defines:

- **Request schemas**: Field names, types, required/optional status, validation rules
- **Response schemas**: Expected data structures
- **Validation constraints**: Min/max values, string patterns, enum values

**Form Implementation Requirements**:

- Form fields must match the OpenAPI schema field names exactly
- Required fields must be marked as required and validated before submission
- Field types must match the OpenAPI schema (string, number, boolean, array, object)
- Validation rules (e.g., minLength, maxLength, pattern) must be implemented as specified in the OpenAPI schema
- Enum values must be restricted to the values defined in the OpenAPI schema
- JSON fields (objects/arrays) must be validated for proper JSON syntax
- Error messages should reflect validation failures from the API when available

**API Documentation**:

- OpenAPI JSON: Available at `/v3/api-docs` when backend is running
- OpenAPI YAML: Available at `/v3/api-docs.yaml`
- Swagger UI: Interactive documentation at `/swagger-ui/index.html`

## General layout

Layout consists of a topbar, under that the main content window, and at the bottom there is a footer.

## Topbar

Topbar has navigation buttons Entity, Policy located to the left.

- Navigation buttons show active state (flat variant) when on corresponding route
- Name of the selected organization shall be displayed in the topbar centered
- Organization dropdown menu should be placed in the top bar to the right (if multiple organizations)
- Logout button is placed to the very right

- Left: logomark + "Sweden Connect OIDF Portal" wordmark (--brand-sage, semibold)
- Center (authenticated pages only): current org name (semibold, --ink)
- Right: org selector dropdown (authenticated) + "SV | EN" language toggle
- Below logo row: horizontal nav links (medium weight, --muted; active link underlined
  in --brand-sage)
- Below nav (multi-step pages only): step breadcrumb trail with › separators
-

## Footer

- **Placement**: Sticky at the bottom on all pages
- **Content**: Text "OpenID Federation Admin SwedenConnect Copyright 2026" right-aligned
- **Visibility**: Always visible, including on the login page

## Page Colors

- Take the color schema from https://www.swedenconnect.se/

## StartView / Login View

The start view should display a login button that triggers the OIDC login flow.
When backend server returns a 401 http status this page should be displayed so that the user can login again.

- Centered card layout with title "OpenID Federation - Administration"
- Login button prominently displayed
- Message: "Please log in to continue."

## Logout function

In the topbar to the very right a button should be placed with the functionality to logout the current user.
When pressed logout is performed and the startpage is displayed to the user.

## Organization selection

There shall be a dropdown menu filled with different organisations that is loaded from backend. When an organization is
selected it must be written to the server then navigate to entity view and reload data from server.

Organization dropdown menu should be placed in the top bar to the right.

- If there is only one organization the organization dropdown menu should not be shown.
- If only one organization exists and none is selected, it should be auto-selected
- Name of the selected organization shall be displayed in the topbar centered
- **Behavior**: When an organization is selected, the selection is saved to the server via PUT request, then a full page
  reload (`location.reload()`) is performed. After reload, the page navigates to the Entity List View with the new
  organization's data loaded.

## Entity List View

The Entity List View displays all entities (Federation and Hosted) in a table format.

### Table Structure

- **Columns**: Entity Identifier, Modules, Actions
- **Entity Types**: Federation Entity and Hosted Entity are displayed together
- **Empty State**: Message "No entities found." when no entities exist

### Modules Display

Available modules for each Federation Entity are displayed as buttons:

- Trustanchor (if module exists)
- Intermediate (if module exists)
- Resolver (if module exists)
- Trustmark Issuer (if module exists)

**Module Navigation Details**:

- For Trustanchor, Intermediate: Clicking the button navigates to the Subordinates List View
- For Trustmark Issuer: Clicking the button navigates to the Trustmarks List View
- Navigation URL format for subordinates:
  `/entities/[entityId]/modules/[moduleType]/subordinates?taImId=[moduleId]&issuer=[entityIdentifier]`
- Navigation URL format for trustmarks:
  `/entities/[entityId]/modules/trustmarkissuer/trustmarks?trustmarkIssuerId=[moduleId]`

### Actions

- **Add Federation Entity**: Button to create new Federation Entity
- **Add Hosted Entity**: Button to create new Hosted Entity
- **Edit**: Button per entity (only for Federation and Hosted entities) - navigates to edit view
- **Delete**: Button per entity (only for Federation and Hosted entities) - opens confirmation dialog

## Entity Create Views

### Federation Entity Create

Form to create a new Federation Entity.

**Form Fields** (must follow OpenAPI schema from backend API):

- Entity Identifier (required, text field)
    - Pre-filled with entityPrefix from user store if available
    - Validation: Required field (as per OpenAPI schema)
    - Field name must match OpenAPI schema exactly
- Crit (multi-select combobox with chips)
    - Allows multiple values
    - Chips can be removed individually
    - Field name and type must match OpenAPI schema

**Buttons**:

- Cancel: Returns to Entity List View
- Create: Saves entity and navigates back to Entity List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Entity Identifier is required (as per OpenAPI schema)
- Form validation prevents submission if required fields are missing
- API validation errors are displayed in the error banner

### Hosted Entity Create

Form to create a new Hosted Entity.

**Form Fields** (must follow OpenAPI schema from backend API):

- Entity Identifier (required, text field)
    - Validation: Required field (as per OpenAPI schema)
    - Field name must match OpenAPI schema exactly
- Metadata (required, textarea)
    - JSON format validation
    - Must match the object structure defined in OpenAPI schema
    - Monospace font for better readability
    - Auto-growing textarea
    - Field name and type must match OpenAPI schema
- TrustMark Sources (grouped list, reusable component: `TrustmarkSourcesField`)
    - **API schema**: Array of `TrustmarkSourceDto` objects, each with:
        - `trustMarkIssuer` (String) — entity identifier (URL) of the trust mark issuer
        - `trustmarkId` (String) — identifier (URL) of the trustmark to include
    - **Grouped UI**: Entries are grouped by issuer in a hierarchical layout
    - **Visual layout**:
        - Each issuer is displayed as a bordered card/section with:
            - Issuer URL shown as a header (text field, free-form URL)
            - A "Remove" button to delete the issuer and all its trustmarks
            - Below the header: list of trustmark IDs as removable chips
            - An "Add Trustmark" button to add a new trustmark ID under that issuer
        - An "Add Issuer" button at the bottom to add a new issuer section
    - **Data mapping**: The grouped UI maps to/from the flat API array:
        - Display: flat array `[{trustMarkIssuer: A, trustmarkId: 1}, {trustMarkIssuer: A, trustmarkId: 2}]` → grouped
          as Issuer A with trustmarks [1, 2]
        - Submit: grouped structure flattened back to array of `TrustmarkSourceDto` objects
    - **Interaction**:
        - "Add Issuer" → adds empty issuer section with text field for issuer URL
        - "Add Trustmark" (per issuer) → adds text field for trustmark ID
        - Removing a chip/trustmark removes one `TrustmarkSourceDto` entry
        - Removing an issuer removes all associated entries
    - **Validation**: Both `trustMarkIssuer` and `trustmarkId` are free-form URL strings, no backend validation
      constraints
    - **Reusability**: Implemented as a standalone component (`TrustmarkSourcesField.vue`) used in both Hosted Entity
      Create and Edit views

**Buttons**:

- Cancel: Returns to Entity List View
- Create: Saves entity and navigates back to Entity List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Entity Identifier is required (as per OpenAPI schema)
- Metadata must be valid JSON format matching the OpenAPI schema structure
- Form validation prevents submission if validation fails
- API validation errors are displayed in the error banner

## Entity Edit Views

### Federation Entity Edit

Form to edit an existing Federation Entity.

**Entity Form Fields** (same as Create):

- Entity Identifier (required, text field)
- Crit (multi-select combobox with chips)

**Module Configuration Section**:
Modules are configured using expansion panels. Each module type has its own panel.

**Module Types and Configuration**:

1. **Trustanchor**
    - Active switch (toggle on/off)
    - Trust Mark Issuers (multi-select combobox with chips)
    - Status indicator chip: Active (green), Inactive (yellow), or Not configured (grey)

2. **Intermediate**
    - Active switch (toggle on/off)
    - Status indicator chip: Active (green), Inactive (yellow), or Not configured (grey)

3. **Resolver**
    - Active switch (toggle on/off)
    - Resolve Response Duration (text field, e.g., PT1H)
    - Trust Anchor URL (text field)
    - Trusted Keys (textarea, JWKS format, monospace font)
    - Step Retry Duration (text field, e.g., PT1M)
    - Status indicator chip: Active (green), Inactive (yellow), or Not configured (grey)

4. **Trustmark Issuer**
    - Active switch (toggle on/off)
    - Trust Mark Token Validity Duration (text field, e.g., PT1H)
    - Status indicator chip: Active (green), Inactive (yellow), or Not configured (grey)

**Module Constraints**:

- Only one module from Group 1 can be active: Trustanchor OR Intermediate (not both)
- Only one module from Group 2 can be active: Resolver OR Trustmark Issuer (not both)
- Error message displayed if user tries to add conflicting module

**Actions**:

- Cancel: Returns to Entity List View
- Save Entity: Saves entity data (Entity Identifier and Crit)
- Save (per module): Saves individual module configuration
- Delete (per module): Opens confirmation dialog to delete module

**Module Delete Confirmation**:

- Dialog: "Are you sure you want to delete the [module type] module? This action cannot be undone."
- Buttons: Cancel, Yes Delete (with loading state)

### Hosted Entity Edit

Form to edit an existing Hosted Entity.

**Form Fields** (same as Create):

- Entity Identifier (required, text field)
- Metadata (required, textarea with JSON validation, monospace font)
- TrustMark Sources (grouped list using `TrustmarkSourcesField` component, same as Create)

**Buttons**:

- Cancel: Returns to Entity List View
- Save: Saves entity and navigates back to Entity List View on success

## Entity Delete

Delete functionality for entities with confirmation dialog.

**Trigger**: Delete button in Entity List View Actions column

**Confirmation Dialog**:

- Title: "Confirm Delete"
- Message: "Are you sure you want to delete entity [entity identifier]? This action cannot be undone."
- Buttons:
    - Cancel: Closes dialog
    - Yes, Delete: Performs deletion (with loading state, disabled during operation)

**After Deletion**:

- Dialog closes
- Entity List View is refreshed
- Success: Entity removed from list

## Policy List View

The Policy List View displays all policies in a table format.

### Table Structure

- **Columns**: Name, Actions
- **Empty State**: Message "No policies found." when no policies exist

### Actions

- **Add**: Button to create new Policy
- **Edit**: Button per policy - navigates to edit view
- **Delete**: Button per policy - opens confirmation dialog

## Policy Create View

Form to create a new Policy.

**Form Fields** (must follow OpenAPI schema from backend API):

- Policy Name (required, text field)
    - Validation: Required field (as per OpenAPI schema)
    - Field name must match OpenAPI schema exactly
- Policy JSON (required, textarea)
    - JSON format validation
    - Must match the object structure defined in OpenAPI schema
    - Monospace font for better readability
    - Auto-growing textarea
    - Field name and type must match OpenAPI schema

**Buttons**:

- Cancel: Returns to Policy List View
- Create: Saves policy and navigates back to Policy List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Policy Name is required (as per OpenAPI schema)
- Policy JSON must be valid JSON format matching the OpenAPI schema structure
- API validation errors are displayed in the error banner

## Policy Edit View

Form to edit an existing Policy.

**Form Fields** (same as Create):

- Policy Name (required, text field)
- Policy JSON (required, textarea with JSON validation, monospace font)

**Buttons**:

- Cancel: Returns to Policy List View
- Save: Saves policy and navigates back to Policy List View on success

## Policy Delete

Delete functionality for policies with confirmation dialog.

**Trigger**: Delete button in Policy List View Actions column

**Confirmation Dialog**:

- Title: "Confirm Delete"
- Message: "Are you sure you want to delete policy [policy name]? This action cannot be undone."
- Buttons:
    - Cancel: Closes dialog
    - Yes, Delete: Performs deletion (with loading state, disabled during operation)

**After Deletion**:

- Dialog closes
- Policy List View is refreshed
- Success: Policy removed from list

## Subordinates List View

The Subordinates List View displays all subordinates for a specific module (Trustanchor or Intermediate).

**Access**: Navigated to from Entity List View by clicking a module button (Trustanchor or Intermediate).

**URL Format**: `/entities/[entityId]/modules/[moduleType]/subordinates?taImId=[moduleId]&issuer=[entityIdentifier]`

### Table Structure

- **Columns**: Entity Identifier, Actions
- **Loading State**: Progress circular indicator with text "Loading subordinates..." while data is being fetched
- **Empty State**: Message "No subordinates found." when no subordinates exist
- **Data Loading**: Subordinates are loaded from the module endpoint (trust-anchor or intermediate) using the `taImId`
  query parameter. The full subordinate object from the API is preserved (all fields available for display).

### Status Icons

The Status column displays small icons indicating the state of each subordinate.
Icons have hover tooltips explaining their meaning. Multiple icons can appear for the same subordinate.

- **EC Location** (`mdi-link`):
    - Shown when `ecLocation` has a value OR `ecLocationAutomaticResolve` is `true`
    - Tooltip: "EC Location configured"
    - Indicates that an entity configuration location is set (either explicit URL or auto-resolved from a hosted entity
      with the same subject name)

- **Remote entity** (`mdi-cloud-outline`):
    - Shown when `entityIdentifier` does NOT start with `entityPrefix` (from userStore)
    - Tooltip: "Remote entity"
    - Indicates that the subordinate belongs to an external organization/domain, not the local federation entity

### Actions

- **Add Subordinate**: Button to create new Subordinate
- **Back**: Button that navigates back to Entity List View (home)
- **Edit**: Button per subordinate - navigates to edit view
- **Delete**: Button per subordinate - opens confirmation dialog

## Subordinate Create View

Form to create a new Subordinate for a module.

**Access**: Navigated to from Subordinates List View by clicking "Add Subordinate" button.

**URL Format**: `/entities/[entityId]/modules/[moduleType]/subordinates/new?taImId=[moduleId]`

**Form Fields** (must follow OpenAPI schema from backend API):

- Entity Identifier (Subject) (required, text field)
    - Label: "Entity Identifier (Subject)"
    - Hint: "Subject entity identifier (required, URL)"
    - Validation: Required field
- Policy (optional, select dropdown)
    - Label: "Policy"
    - Hint: "Policy (UUID)"
    - Options: Loaded from policies list
    - Clearable: Yes
- JWKS (Public Keys) (required, textarea)
    - Label: "JWKS (Public Keys)"
    - Hint: "Public keys in JWKS format (required, JSON)"
    - Validation: Required field, must be valid JSON format
    - Monospace font for better readability
    - Auto-growing textarea
    - **JWKS Loader Button**: A "Load JWKS" button is placed in proximity to the JWKS field (typically below the field
      or as an append-inner icon button)
        - Button label: "Load JWKS"
        - Button style: Secondary variant (v-btn with color="secondary" or variant="outlined")
        - **Functionality**:
            - Reads the value from the Entity Identifier (Subject) field
            - Makes a POST request to `/admin/support/jwks` endpoint with the entity identifier as the request body (
              plain text string)
            - **Disabled state**: Button is disabled when Entity Identifier field is empty or invalid
            - **Loading state**: Button shows loading indicator (spinner) and is disabled while the request is in
              progress
        - **Success behavior** (HTTP 200):
            - Response is a JSON object containing the JWKS
            - JWKS is formatted as pretty-printed JSON (2 spaces indentation)
            - Formatted JWKS value is automatically set in the JWKS textarea field
            - Any existing content in the JWKS field is replaced
        - **Error handling**:
            - If request fails (non-200 status), error message is displayed in the error banner
            - Common error messages from backend:
                - "Entity configuration is missing claims"
                - "Entity configuration is missing jwks"
                - "Unable to get entity configuration."
                - "Invalid entity ID: [entityId]"
            - JWKS field is not modified on error
            - User can retry after correcting the Entity Identifier
- Metadata Policy Crit (optional, multi-select combobox with chips)
    - Label: "Metadata Policy Crit"
    - Hint: "MetadataPolicyCrit (list)"
    - Allows multiple values
    - Chips can be removed individually
- Crit (optional, multi-select combobox with chips)
    - Label: "Crit"
    - Hint: "Crit (list)"
    - Allows multiple values
    - Chips can be removed individually
- EC Location (optional, text field)
    - Label: "EC Location"
    - Hint: "Ec Location, expressed as an url"
- EC Location Automatic Resolve (optional, switch)
    - Label: "EC Location Automatic Resolve"
    - Hint: "System will try to find hosted entity with same subject name"
    - Default: false

**Buttons**:

- Cancel: Returns to Subordinates List View (preserves query parameters)
- Create: Saves subordinate and navigates back to Subordinates List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Entity Identifier is required (as per OpenAPI schema)
- JWKS must be valid JSON format matching the OpenAPI schema structure
- Form validation prevents submission if validation fails
- API validation errors are displayed in the error banner

## Subordinate Edit View

Form to edit an existing Subordinate.

**Access**: Navigated to from Subordinates List View by clicking "Edit" button.

**URL Format**: `/entities/[entityId]/modules/[moduleType]/subordinates/[subordinateId]/edit?taImId=[moduleId]`

**Form Fields** (same as Create, plus):

- Effective EC Location (read-only, text field)
    - Label: "Effective EC Location"
    - Hint: "Calculated server-side"
    - Disabled: Yes
    - Only shown if value exists
- **JWKS Loader Button**: Same functionality as in Subordinate Create View
    - Reads Entity Identifier value from the form
    - Loads JWKS from backend and populates the JWKS field
    - Can be used to refresh/update JWKS if Entity Identifier is changed
    - Replaces existing JWKS content when new JWKS is loaded

**Buttons**:

- Cancel: Returns to Subordinates List View (preserves query parameters)
- Save: Saves subordinate and navigates back to Subordinates List View on success

**Loading State**:

- Progress circular indicator with text "Loading subordinate..." while fetching data

**Data Loading**:

- Subordinate data is loaded on mount
- Policies are loaded on mount to populate the Policy dropdown

## Subordinate Delete

Delete functionality for subordinates with confirmation dialog.

**Trigger**: Delete button in Subordinates List View Actions column

**Confirmation Dialog**:

- Title: "Confirm Delete"
- Message: "Are you sure you want to delete subordinate \"[entity identifier]\"? This action cannot be undone."
- Buttons:
    - Cancel: Closes dialog
    - Yes, Delete: Performs deletion (with loading state, disabled during operation)

**After Deletion**:

- Dialog closes
- Subordinates List View is refreshed
- Success: Subordinate removed from list

## Trustmarks List View

The Trustmarks List View displays all trustmarks for a specific Trustmark Issuer module.

**Access**: Navigated to from Entity List View by clicking the Trustmark Issuer module button.

**URL Format**: `/entities/[entityId]/modules/trustmarkissuer/trustmarks?trustmarkIssuerId=[moduleId]`

### Table Structure

- **Columns**: Trustmark Type (or ID), Actions
- **Loading State**: Progress circular indicator with text "Loading trustmarks..." while data is being fetched
- **Empty State**: Message "No trustmarks found." when no trustmarks exist
- **Data Loading**: Trustmarks are loaded from `/api/v1/trustmarks` endpoint, filtered by trustmarkIssuerId

### Actions

- **Add Trustmark**: Button to create new Trustmark
- **Back**: Button that navigates back to Entity List View (home)
- **Edit**: Button per trustmark - navigates to edit view
- **Delete**: Button per trustmark - opens confirmation dialog
- **View Subjects**: Clicking on a trustmark row or button navigates to Trustmark Subjects List View

## Trustmark Create View

Form to create a new Trustmark for a Trustmark Issuer module.

**Access**: Navigated to from Trustmarks List View by clicking "Add Trustmark" button.

**URL Format**: `/entities/[entityId]/modules/trustmarkissuer/trustmarks/new?trustmarkIssuerId=[moduleId]`

**Form Fields** (must follow OpenAPI schema from backend API):

- Trustmark Issuer ID (required, hidden/read-only)
    - Automatically set from query parameter
- Trustmark Type (required, text field)
    - Label: "Trustmark Type"
    - Hint: "Trustmark entityid, ex https://sc.swedenconnect.se/loa3"
    - Validation: Required field (as per OpenAPI schema)
    - Field name must match OpenAPI schema exactly
- Additional fields as defined in OpenAPI schema

**Buttons**:

- Cancel: Returns to Trustmarks List View (preserves query parameters)
- Create: Saves trustmark and navigates back to Trustmarks List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Required fields must be filled (as per OpenAPI schema)
- Form validation prevents submission if validation fails
- API validation errors are displayed in the error banner

## Trustmark Edit View

Form to edit an existing Trustmark.

**Access**: Navigated to from Trustmarks List View by clicking "Edit" button.

**URL Format**:
`/entities/[entityId]/modules/trustmarkissuer/trustmarks/[trustmarkId]/edit?trustmarkIssuerId=[moduleId]`

**Form Fields** (must follow OpenAPI schema from backend API):

- Same fields as Create View
- Fields are pre-filled with existing trustmark data

**Buttons**:

- Cancel: Returns to Trustmarks List View (preserves query parameters)
- Save: Saves trustmark and navigates back to Trustmarks List View on success

**Data Loading**:

- Trustmark data is loaded on mount from `/api/v1/trustmarks/[trustmarkId]`

## Trustmark Delete

Delete functionality for trustmarks with confirmation dialog.

**Trigger**: Delete button in Trustmarks List View Actions column

**Confirmation Dialog**:

- Title: "Confirm Delete"
- Message: "Are you sure you want to delete trustmark \"[trustmark type/identifier]\"? This action cannot be undone."
- Buttons:
    - Cancel: Closes dialog
    - Yes, Delete: Performs deletion (with loading state, disabled during operation)

**After Deletion**:

- Dialog closes
- Trustmarks List View is refreshed
- Success: Trustmark removed from list

## Trustmark Subjects List View

The Trustmark Subjects List View displays all subjects for a specific trustmark.

**Access**: Navigated to from Trustmarks List View by clicking on a trustmark row or "View Subjects" button.

**URL Format**: `/entities/[entityId]/modules/trustmarkissuer/trustmarks/[trustmarkId]/subjects`

### Table Structure

- **Columns**: Subject Identifier (or similar field from OpenAPI schema), Actions
- **Loading State**: Progress circular indicator with text "Loading trustmark subjects..." while data is being fetched
- **Empty State**: Message "No trustmark subjects found." when no subjects exist
- **Data Loading**: Trustmark subjects are loaded from `/api/v1/trustmarks/[trustmarkId]/subjects` endpoint

### Actions

- **Add Trustmark Subject**: Button to create new Trustmark Subject
- **Back**: Button that navigates back to Trustmarks List View (preserves query parameters)
- **Edit**: Button per subject - navigates to edit view
- **Delete**: Button per subject - opens confirmation dialog

## Trustmark Subject Create View

Form to create a new Trustmark Subject for a trustmark.

**Access**: Navigated to from Trustmark Subjects List View by clicking "Add Trustmark Subject" button.

**URL Format**: `/entities/[entityId]/modules/trustmarkissuer/trustmarks/[trustmarkId]/subjects/new`

**Form Fields** (must follow OpenAPI schema from backend API):

- Trustmark ID (required, hidden/read-only)
    - Automatically set from URL parameter
- Subject fields as defined in OpenAPI schema
    - Field names must match OpenAPI schema exactly
    - Validation rules must follow OpenAPI specification

**Buttons**:

- Cancel: Returns to Trustmark Subjects List View
- Create: Saves trustmark subject and navigates back to Trustmark Subjects List View on success

**Validation**:

- All validation rules must follow the OpenAPI specification
- Required fields must be filled (as per OpenAPI schema)
- Form validation prevents submission if validation fails
- API validation errors are displayed in the error banner

## Trustmark Subject Edit View

Form to edit an existing Trustmark Subject.

**Access**: Navigated to from Trustmark Subjects List View by clicking "Edit" button.

**URL Format**: `/entities/[entityId]/modules/trustmarkissuer/trustmarks/[trustmarkId]/subjects/[subjectId]/edit`

**Form Fields** (must follow OpenAPI schema from backend API):

- Same fields as Create View
- Fields are pre-filled with existing subject data

**Buttons**:

- Cancel: Returns to Trustmark Subjects List View
- Save: Saves trustmark subject and navigates back to Trustmark Subjects List View on success

**Data Loading**:

- Trustmark subject data is loaded on mount from `/api/v1/trustmarks/subjects/[subjectId]`

## Trustmark Subject Delete

Delete functionality for trustmark subjects with confirmation dialog.

**Trigger**: Delete button in Trustmark Subjects List View Actions column

**Confirmation Dialog**:

- Title: "Confirm Delete"
- Message: "Are you sure you want to delete trustmark subject \"[subject identifier]\"? This action cannot be undone."
- Buttons:
    - Cancel: Closes dialog
    - Yes, Delete: Performs deletion

**After Deletion**:

- Dialog closes
- Trustmark Subjects List View is refreshed
- Success: Trustmark subject removed from list

## Error Handling

### Error Banner

Error messages are displayed in a banner located under the topbar, above the main content.

**Characteristics**:

- Type: error (red color)
- Closable: Yes, user can close with X button
- Auto-dismiss: close after 30sec
- Visibility: Shown when backend returns errors or validation fails

**Error Messages Examples**:

- "Invalid JSON format"
- "Entity ID not found"
- "This field is required"
- "Invalid JSON in metadata field"
- "Invalid JSON in policy field"
- Module constraint violations (e.g., "Cannot add Trustanchor when Intermediate already exists")

### HTTP Status Handling

- **401 Unauthorized**: Automatic redirect to login page
- Other errors: Displayed in error banner

### Form Validation

- Real-time validation with error messages displayed directly under fields
- Required fields show error if empty
- JSON fields show error if format is invalid
- Form submission is prevented if validation fails
- Validation errors are cleared when user corrects the input

### Loading and Disabled States

- Buttons and form fields are disabled during save/delete operations
- Loading indicators show progress during async operations
- Prevents multiple simultaneous submissions

## Breadcrumbs Navigation

**Placement**: Under topbar, above the page's main content

**Format**: Home > Section > Subsection (e.g., "Entity > Edit Federation Entity")

**Characteristics**:

- Each level is clickable for navigation back
- Visual separator between levels (typically ">")

**Display Rules**:

- Home page: No breadcrumb shown
- List pages: "Entity" or "Policy"
- Create pages: "Entity > Create Federation Entity" or "Policy > Create Policy"
- Edit pages: "Entity > Edit Federation Entity" or "Policy > Edit Policy"
- Module/Subordinate pages: "Entity > [Entity Identifier] > [Module Type] > Subordinates"

## Technical Details

### Loading States

- Progress circular indicators displayed while data is being loaded
- Loading text accompanies indicators (e.g., "Loading entities...", "Loading entity...", "Loading policies...")
- Full page or section-level loading states depending on context

### Disabled States

- Buttons and form fields are disabled during save/delete operations
- Prevents user interaction during async operations
- Visual feedback (greyed out) indicates disabled state

### Navigation

- Automatic redirect after successful create/edit/delete operations
- Navigation returns to appropriate list view
- Browser back button works as expected

### Form Validation

- Real-time validation with immediate feedback
- Error messages displayed under invalid fields
- Form submission blocked until all validations pass
- JSON fields validated for proper syntax

### JSON Fields

- Monospace font (Monaco, Menlo, Ubuntu Mono) for better readability
- Auto-growing textareas for flexible content
- Syntax validation before submission