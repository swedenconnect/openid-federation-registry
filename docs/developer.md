![Logo](../docs/images/sweden-connect.png)
# Developer Guide: Entity Registry for OpenID Federation

This document provides instructions on how to set up a development environment for the OpenID Federation Entity Registry application.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **Java SDK 21**
*   **Maven** (3.6.3 or later)
*   **Docker** (with Docker Compose)
*   **Git**

## Project Structure

The project is organized as follows:

*   `service`: Contains the Spring Boot application source code.
*   `config/local`: Contains Docker Compose configuration for setting up local dependencies.
*   `docs`: Project documentation.

## Building the Project

To build the project and run the unit tests, run the following command from the project root:
```shell
mvn clean install
```


## Running Locally

To run the application locally, you need to start the supporting dependencies and then launch the Spring Boot application
with the `docker` profile.

### 1. Start the Local Environment

The application requires a MariaDB database, as well as signing keys and a resource server public key.
A setup script is provided in the `config/local` directory to generate these keys and start the environment.

1.  Navigate to the local configuration directory:
```shell
cd config/local
 ```

2.  Run the setup script:
```shell
sh setup.sh
```

This script will:
*   Generate a self-signed certificate and a PKCS12 keystore (`snake-oil.p12`) for signing.
*   Extract the public key (`snake-oil.pem`) for the resource server configuration.
*   Start the MariaDB instance using Docker Compose.

To stop the docker compose environment:
```shell
docker compose down
   ```

### 2. Start the Application

You can run the application using Maven or your IDE. 

**Using Maven:**

From the project root run:
```shell
mvn spring-boot:run -pl service \
-Dspring-boot.run.arguments="--spring.profiles.active=docker --spring.config.import=../config/local/application-docker.yml"
```


**Using IntelliJ IDEA:**

1.  Create a generic "Application" or "Spring Boot" run configuration.
2.  Set the **Main Class** to `se.swedenconnect.oidf.registry.RegistryApplication` 
3.  In **Program Arguments** add:```--spring.profiles.active=docker --spring.config.import=config/local/application-docker.yml```
4.  Run the configuration.

### 3. Verify Setup

Once the application is running, you can access the following endpoints:

*   **Health Check:** [localhost:8011/actuator/health](http://localhost:8011/actuator/health)
*   **API Documentation:** [localhost:8010/swagger-ui/index.html](http://localhost:8010/swagger-ui/index.html)
*   **OpenAPI Specification:** [localhost:8010/v3/api-docs](http://localhost:8010/v3/api-docs)

> **WARNING:** This Docker Compose setup is intended **strictly for local testing/development**. It uses HTTP, self-signed certificates,
> and default passwords. A production or serious development setup mandates HTTPS, valid certificates, and hardened security configurations.

---
## Configuration

The local environment configuration is primarily handled in:

*   `config/local/application-docker.yml`: Overrides for the local profile (server port, database connection, logging).
*   `config/local/docker-compose.yml`: Container definitions for local dependencies.

---
## Coding Style and Contribution

When contributing to this project:

1. Follow the existing code style and conventions
2. Write tests for new features
3. Update documentation as needed
4. Ensure all tests pass before submitting a pull request

Read more in the [Contributing Guide](../CONTRIBUTING.md).

> **Note:** Don't forget to run mvn verify (or mvn install) to activate the style checks. These will be run automatically
> by GitHub Actions, and if the checks fail, the pull request will not be merged.

The Maven-driven code style rules can be viewed at the
[openid-federation-commons](https://github.com/swedenconnect/openid-federation-commons/blob/main/pom.xml#L156) repository.
*   This project uses **Lombok**. Ensure your IDE has the Lombok plugin installed and annotation processing enabled.

---
## Additional Resources
- [Configuration guide](configuration.md)
- [Audit Event documentation](audit.md)
- [Validation documentation](validation.md)
---

Copyright &copy; 2025, [SwedenConnect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
