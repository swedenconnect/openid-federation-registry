![Logo](../docs/images/sweden-connect.png)

# Validation

This document describes the validation rules available for validating properties in the OpenID Federation Entity
Registry Service.

## Table of Contents

- [Overview](#overview)
- [Validation Rules](#validation-rules)
- [Combined Validation Examples](#combined-validation-examples)

---

## Overview

Properties can have validation rules applied and combined using pipe ('|') syntax. For example:
`"required|length:2,10|email"` means a value must exist, be between 2-10 characters, and be a valid email format.

### Syntax

The syntax for validator configuration is: `{rule}:{config}`

Multiple rules can be combined using the pipe character (`|`).

## Validation Rules

| Rule         | Config         | Description                                                                  | Example           |
|--------------|----------------|------------------------------------------------------------------------------|-------------------|
| required     | {no options}   | Ensures value is not null or blank (whitespace trimmed)                      | required          |
| min          | numeric value  | Validates number is greater than specified value                             | min:10            |
| max          | numeric value  | Validates number is less than specified value                                | max:100           |
| length       | min,max        | Ensures string length is between min and max values                          | length:3,10       |
| json         | {no options}   | Validates string is valid JSON format                                        | json              |
| jwk          | {no options}   | Validates string is valid JWK format                                         | jwk               |
| jwks         | public,kid,req | Validates JWKS: req=requires keys, public=public keys only, kid=requires kid | jwks:public       |
| email        | {no options}   | Validates string is valid email format                                       | email             |
| ends_with    | suffix string  | Validates string ends with specified suffix                                  | ends_with:.com    |
| starts_with  | prefix string  | Validates string starts with specified prefix                                | starts_with:https |
| contains     | substring      | Validates string contains specified substring                                | contains:test     |
| alpha        | {no options}   | Validates string contains only letters                                       | alpha             |
| alphanumeric | {no options}   | Validates string contains only letters and numbers                           | alphanumeric      |
| number       | {no options}   | Validates value is a valid number (includes decimals)                        | number            |
| date         | {no options}   | Validates string is valid date in YYYY-MM-DD format                          | date              |
| between      | min,max        | Validates number is between min and max values                               | between:1,100     |
| url          | {no options}   | Validates string is valid URL format                                         | url               |
| matches      | regex pattern  | Validates string matches specified regex pattern                             | matches:^[A-Z]+$  |

## Combined Validation Examples

```text
"required|length:3,50|email" - Required email between 3-50 characters
"number|between:0,100" - Optional number between 0 and 100
"required|url|starts_with:https" - Required HTTPS URL
"required|json|jwks:public" - Required JWKS with public keys only
```

---

Copyright &copy; 2025, [SwedenConnect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
