![Logo](../docs/images/sweden-connect.png)

# OIDF Entity Registry Service

A SpringBoot REST-service implementing the OIDF Entity Registry API

---

## About

### Overview

This project is a Spring Boot service designed to store and serve OpenID Connect Federation entity configurations.
The service provides RESTful APIs for creating, retrieving, updating, and managing OpenID Connect Federation entities.

### Features

- **Create Entity**: Add new entities with their configurations.
- **Retrieve Entities**: Fetch all entities or a specific entity by ID.
- **Update Entity**: Update the details of an existing entity.
- **Policies**: FederationPolicies
- **TrustMarkSubjects**: Assign TrustMarks to subjects
- **Storage Options**: Supports either file-based storage using a `HashMap` or MariaDB database.
- **Docker Compose Integration**: Uses Spring Boot 3's new support for running Docker Compose files.
- **OpenAPI Documentation**: Integrated with `springdoc-openapi-starter-webmvc-ui` for API documentation.

### Prerequisites

- Java 21 or later
- Maven 3.8.7 or later
- Docker

### Getting Started

#### Clone the Repository

```sh
git clone https://github.com/swedenconnect/openid-federation-registry
cd openid-federation-registry
```

#### Build the Project

```sh
mvn clean install
```

#### Run the Application

Registry is part of OpenId Federation and are called by both
the [Federation Nodes](https://github.com/swedenconnect/openid-federation-services), and
the [Registry Admin Tool](https://github.com/swedenconnect/openid-federation-registry-admin). The Registry is also
dependent on both a SQL database and Redis.
The easiest way to handle all those dependencies is to run the Docker
compose [local-environment](https://github.com/swedenconnect/local-environment) for OpenID Federation.

```sh
cd ../
git clone git@github.com:swedenconnect/local-environment.git
cd local-environment/openidfederation
docker compose up -d
```

Since we are going to work on Registry, we need to stop that container:

```sh
docker compose down oidf-registry
```

Or locally add "excluded" to oidf-registry in the compose.yml file, before starting docker compose.

```
oidf-registry:
    profiles: ["excluded"]
    ...
```

Then start the Registry application by:

```sh
cd ../openid-federation-registry/service
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.import=../config/local/application-local.yml"
```

#### Viewing API Documentation

This project uses `springdoc-openapi-starter-webmvc-ui` to generate API documentation. Once the application is running,
you can view the API documentation in the Swagger and OpenAPI UI at the following URLs:

- http://localhost:8010/swagger-ui/index.html
- http://localhost:8010/v3/api-docs

#### Building and Deploying

For detailed instructions on building and deploying the service, refer to
the [developer documentation](../docs/developer.md).

## Configuration

### Security Configuration

> TODO

### Contributing

> TODO

## License

The OIDF Entity Registry is Open Source software released under
the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

-----

Copyright &copy; 2025, [Swedenconnect](http://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
