# Sweden Connect Profile

Templates for running the registry as a Sweden Connect Trust Anchor or Registration Intermediate. The registry's code
holds no Sweden Connect settings; the profile in [`profiles/sweden-connect`](profiles/sweden-connect) adds them through
configuration, and the files can be loaded as they are.

| File                                                                                       | What it is                                                                                                            |
|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| [`application-sweden-connect.yml`](profiles/sweden-connect/application-sweden-connect.yml) | Configures the two templates below under `openid.federation.registry.templates`.                                      |
| [`registration-template.json`](profiles/sweden-connect/registration-template.json)         | `step` template for the Metadata Policy Creation step of a Registration Intermediate.                                 |
| [`trust-anchor-policy.json`](profiles/sweden-connect/trust-anchor-policy.json)             | `policy` template: the metadata policy a Sweden Connect Trust Anchor applies to relying parties and OpenID Providers. |

## Load the profile

Import the configuration next to the registry's own, from the repository root:

```shell
--spring.config.import=file:docs/profiles/sweden-connect/application-sweden-connect.yml
```

The template files are read from `sweden-connect.profile-directory`, by default `docs/profiles/sweden-connect` relative
to the directory the registry is started from. When the files are elsewhere, for example in a container, set it:

```shell
--sweden-connect.profile-directory=/opt/registry/profiles/sweden-connect
```

The profile sets the whole `templates` list; to add templates of your own, copy its entries into your configuration.

## Registration template

`registration-template.json` implements *Registration Intermediate Metadata Assignment* (OIDC metadata requirements,
section 6.2):

- Organization names in Swedish and English, with the untagged value taken from the registry's legal name.
- `organization_identifier` checked against the organization number as a GLUE URI with ICD `0007` (Swedish
  organization numbers).
- `client_name` and `display_name` pinned with the `value` operator, `display_name` falling back to `client_name`.
- `logo_uri` an HTTPS URL that answers with an image.
- `sector_identifier_uri` pinned, an HTTPS URL.
- `signed_jwks_uri` removed (`value: null`). Not supported yet: the registry leaves the rule out when it loads the
  template, with a warning, see [#304](https://github.com/swedenconnect/openid-federation-registry/issues/304).
- For an OpenID Provider, `acr_values_supported` limited to the Swedish eID assurance levels 2–4.

In the flow builder, open the settings of **MetadataPolicyCreationStep** and choose **Load template** →
`sweden-connect-registration`. See [Metadata Review and Metadata Policy](metadata_policy.md).

## Trust Anchor policy

`trust-anchor-policy.json` is the metadata policy of a Sweden Connect Trust Anchor. For relying parties it requires
the registration parameters (`redirect_uris`, `response_types` with `code`, `private_key_jwt`, `client_uri`,
`contacts`, `logo_uri`) and limits the signing and encryption algorithms; for OpenID Providers it requires the
endpoints and supported values the profile needs, including `S256` PKCE, `sv` and `en` UI locales and request objects.

The registry does not support `require_auth_time` (`one_of: [true]`), `id_token_signed_response_alg` and
`userinfo_signed_response_alg` (`value: null`) yet: it leaves them out when it loads the template, with a warning, see
[#304](https://github.com/swedenconnect/openid-federation-registry/issues/304).

On the Trust Anchor's settings, choose the `sweden-connect-trust-anchor` template under **Metadata policy for
subordinates** and select **Load template**. Every subordinate statement the Trust Anchor issues then carries the
policy, combined with the subordinate's own. See
[Trust Anchor and Intermediate policy](metadata_policy.md#trust-anchor-and-intermediate-policy).
