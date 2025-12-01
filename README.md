![Logo](docs/images/sweden-connect.png)

# OpenID Federation Entity Registry Service

A SpringBoot REST-service implementing the OpenID Federation Entity Registry API

---

## About

### Overview

The **OIDF Entity Registry Service** is a Spring Boot application designed to serve as the central repository for OpenID 
Connect Federation entity configurations. It provides a comprehensive RESTful API for creating, retrieving, and managing 
the lifecycle of federation entities.

As a core component of the OpenID Federation ecosystem, the registry ensures secure interoperability by maintaining 
critical data, including federation policies, entity configurations, and trust mark subjects. 
The service uses a **MariaDB** database for robust and persistent storage, enabling trusted interactions within a federation node.


### Features

- **Create Entity**: Add new entities with their configurations.
- **Retrieve Entities**: Fetch all entities or a specific entity by ID.
- **Update Entity**: Update the details of an existing entity.
- **Policies**: FederationPolicies
- **TrustMarkSubjects**: Assign TrustMarks to subjects


## License

The OIDF Entity Registry is Open Source software released under the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).

## Resources
- [Developer documentation](docs/developer.md)
- [Configuration guide](docs/configuration.md)
- [Audit Event documentation](docs/audit.md)
- [Validation documentation](docs/validation.md)

-----

Copyright &copy; 2025, [Swedenconnect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
