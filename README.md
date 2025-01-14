![Logo](docs/images/sweden-connect.png)

# Entity Registry for OpenID Federation

Entity Registry for an OpenID Federation Service. 

Handles EntityConfiguration, Policys and TrustMarkSubjects

---

## About
The Entity Registry for OpenID Federation is a core component that manages the configuration for OpenID Federation 
services. It is responsible for handling and maintaining entities such as policies, entities, and trustmark subjects, 
ensuring secure and efficient interoperability within the OpenID ecosystem. This registry plays a vital role in 
enabling trusted interactions and processes within a federation node.


## Creating a release

Releases are created by Github-actions on tagged commits.

e.g.

```bash
git tag v0.0.0
git push origin v0.0.0
```

Will result in a release and a docker image with that tag.

## License

The OIDF Entity Registry is Open Source software released under the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

-----

Copyright &copy; 2025, [SwedenConnect](http://www.swedenconnect.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).



