# Metadata Review and Metadata Policy

Two registration flow steps decide what an Intermediate publishes about an entity in its Subordinate Statement:

- **Metadata Review** (`MetadataReviewStep`) shows the operator the metadata the entity registers with and waits for
  approval.
- **Metadata Policy Creation** (`MetadataPolicyCreationStep`) builds the Subordinate Statement's `metadata` and
  `metadata_policy` claims from a configurable template, after checking the entity's metadata against it.

Both come after the step that loads the metadata, `LoadEntityConfigurationStep` (fetched) or
`HostedEntityRegistrationStep` (hosted). A typical flow is:

```
Load Entity Configuration or Hosted Entity Registration → Metadata Review → Metadata Policy Creation → publish
```

## Metadata Review

The step always pauses for review; it has no `manualreview` setting. A flow that should not have its metadata checked
leaves the step out.

The reviewer sees:

| Section                         | Content                                                                                       |
|---------------------------------|-----------------------------------------------------------------------------------------------|
| Source                          | Whether the metadata was fetched from the entity's configuration or came in a join request.   |
| Logo                            | Each `logo_uri`, as the image the registry fetched, with its type and size, or why not fetched. |
| Names                           | Every `*_name` parameter, per language tag.                                                   |
| Links                           | Every other `*_uri` parameter.                                                                |
| Changes to the hosted metadata  | Hosted entities only: what this registration changes compared to what is hosted now.          |
| Full metadata                   | The metadata as JSON.                                                                         |

Fetched entities have no stored earlier copy of their metadata, so they get no diff.

### Fetching images

The registry fetches an image only if all of these hold:

- the URL uses HTTPS,
- its host is a domain, or under a domain, that the tenant operator has **validated** for the entity's organization,
- the server answers `200` with an `image/*` content type, within 10 seconds and 512 KB.

Redirects are not followed. The fetched copy is stored with the review, as a `data:` URI, so the reviewer sees what
was checked, and the Metadata Policy Creation step checks the same copy. An image that does not meet the rules is not
fetched, and the reason is shown instead.

## Metadata Policy Creation

The step's `template` setting holds the rules as JSON. When it is not set, the step uses the default template
(`service/src/main/resources/metadata-policy/default.json`). For a Sweden Connect Intermediate, see the
[Sweden Connect Profile](sweden_connect_profile.md). The template is checked when the flow is saved; a
template that cannot be used is refused with `400` and a list of the problems.

With `manualreview` set, the step pauses after building the claims, and the reviewer sees every check, the values
pinned with the `value` operator, and the claims exactly as they will be published.

In the flow builder, the step's settings open a rules editor: general settings, a tab per entity type, a table of
rules and an editor for the selected rule, with **Load default template**, **Load template** (the `step` templates
configured under `openid.federation.registry.templates`, see [Templates](configuration.md#templates)) and
**Edit as JSON**. A loaded template is copied into the step; a later change to the configured file does not change it.

### Template

```json
{
  "default_language": "sv",
  "languages": ["sv", "en"],
  "on_failure": "fail",
  "entity_types": {
    "openid_relying_party": [ { "parameter": "client_name", "...": "..." } ]
  }
}
```

| Field              | Meaning                                                                                                     |
|--------------------|-------------------------------------------------------------------------------------------------------------|
| `default_language` | Language whose value is copied to the parameter without a language tag. Default `sv`.                       |
| `languages`        | Language tags a multilingual parameter must be given in. Default `["sv", "en"]`.                            |
| `on_failure`       | `fail`: a missing required value or a failed check fails the registration. `review`: the step goes on and the failure is shown to the reviewer as a warning. Default `fail`. |
| `entity_types`     | Rules per entity type. Only the entity types the entity declares are used.                                  |

### Rules

| Field                         | Meaning                                                                                                      |
|-------------------------------|--------------------------------------------------------------------------------------------------------------|
| `parameter`                   | The metadata parameter, without language tag.                                                                |
| `claim`                       | `metadata` sets the value; `metadata_policy` constrains it with `operator`.                                  |
| `operator`                    | For `metadata_policy`: `value`, `add`, `default`, `one_of`, `subset_of`, `superset_of` or `essential`. Default `value`. |
| `source`                      | `entity` (the entity's metadata), `organization` (the registry's organization data) or `static`.            |
| `organization_field`          | For source `organization`: `legal_name`, `organization_number` or `organization_identifier`.                 |
| `icd`                         | ISO 6523 code for `organization_identifier` (`urn:glue:iso6523:<icd>:<number>`). Default `0007`.             |
| `fallback`                    | For source `entity`: another parameter to take the value from when this one is not declared.                 |
| `static_value`                | For source `static`: the value. `essential` takes `true` or `false`.                                         |
| `required`                    | A missing value is a failure.                                                                                |
| `multilingual`                | The parameter is given per language in `languages`; a missing language is a failure.                         |
| `untagged`                    | For multilingual parameters, the value without language tag: `none`, `default_language`, `entity_or_default` (the entity's own, else the default language) or `organization`. Default `default_language`. |
| `untagged_organization_field` | For `untagged: organization`: the organization field.                                                        |
| `checks`                      | Checks made on every value, see below.                                                                       |

### Checks

| Check                  | Options                               | Passes when                                                                 |
|------------------------|---------------------------------------|-----------------------------------------------------------------------------|
| `non_empty`            |                                       | No value is blank.                                                          |
| `https_url`            |                                       | Every value is an HTTPS URL.                                                |
| `image`                |                                       | Every value resolves to an image, under the image fetching rules above.     |
| `matches_organization` | `organization_field`, `icd`           | Every value equals that field of the registry's organization data.          |
| `allowed_values`       | `values`                              | Every value, or every element of a list, is one of `values`.                |

### Output

The `metadata` claim is stored on the subordinate as `{ "<entity type>": { ... } }`, and the `metadata_policy` claim as
`{ "metadata_policy": { "<entity type>": { ... } } }`, the form the subordinate API expects. Every generated policy is
parsed with the OpenID Federation policy language before it is accepted.

### Pinned values

A parameter set by the `metadata` claim or fixed with the `value` operator is pinned: the federation resolves the
subordinate statement's value, whatever the entity publishes. For a self-managed entity the registration API lists
them, with the pinned keys, in `pinnedMetadata`. Running the registration again with a change to a pinned value or key
pauses for review; without one, the review steps are passed. See
[Registration Flow, Alternative flow C](use_case_registration_flow.md#alternative-flow-c--a-self-managed-entity-publishes-again).

## Trust Anchor and Intermediate policy

A Trust Anchor or Intermediate can have a metadata policy of its own (`metadataPolicy` on the module, the same
`{ "metadata_policy": { ... } }` form). Every subordinate statement it issues carries that policy combined with the
subordinate's own, operator by operator as OpenID Federation combines a superior's policy with a subordinate's.

- Saving the module is refused with `400` when its policy is not valid or cannot be combined with the policy of one of
  its subordinates; the message names the subordinate.
- Saving a subordinate, by hand or through the Publish step of a registration flow, is refused (the registration is
  declined) when its policy cannot be combined with the module's.
- A parameter with `"value": null` (remove the parameter) is kept as it is, and combining it with any other operator
  for the same parameter is a conflict.

In the GUI, the module's settings have a **Metadata policy for subordinates** field, with **Load template** for the
`policy` templates configured under `openid.federation.registry.templates`. A loaded template is copied into the
module.
