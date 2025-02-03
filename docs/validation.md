# Validation

On every property a validation rule can be applied, they can be combined by using the following syntax:
"req | min:2 | max:6" this means that a word has to exist and be grater that 2 but less then 6 characters in length.

Syntax for adding con to the rule looks like {rule}:{config} ex min:3

## Description of the validation rules

| Rule   | conf                  | Description                                                                  | Example          |
|--------|-----------------------|------------------------------------------------------------------------------|------------------|
| req    | {no options}          | Requred that the value is not blank, white space is trimmed                  |                  |
| min    | numeric length        | Minimum characterlength                                                      | min:3            |
| max    | numeric length        | Maximum characterlength                                                      | max:5            |
| regexp | ex ^\d+$              | Reg exp that has to match                                                    | regex:^\d+$      |
| jwk    |                       | Test that is is parable                                                      | jwk              |
| jwks   | {none},kid,req,public | req, min one key. public ensure the key is public, kid ensure kid is present | jwks or jwks:kid |


