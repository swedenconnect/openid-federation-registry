![Logo](../docs/images/sweden-connect.png)

# Developer Guide: Entity Registry for OpenID Federation

This document provides instructions on how to set up a development environment for the OpenID Federation Entity Registry application.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [Coding Style and Contribution](#coding-style-and-contribution)
- [GUI Wording](#gui-wording)
- [Additional Resources](#additional-resources)

## Prerequisites

Before you begin, ensure you have the following installed:

* **Java SDK 25**
*   **Maven** (3.6.3 or later)
*   **Docker** (with Docker Compose)
*   **Git**

## Project Structure

The project is organized as follows:

*   `service`: Contains the Spring Boot application source code.
*   `frontend`: The Vue 3 and Vite frontend. It is built by Maven and served from the static resources of the service.
*   `config/local`: Contains Docker Compose configuration for setting up local dependencies.
*   `docs`: Project documentation.

## Building the Project

To build the project and run the unit tests, run the following command from the project root:
```shell
mvn clean install
```

The frontend is built by the [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) as part of the
build of the `service` module. The plugin installs Node under `service/target`, runs `npm ci` and `npm run build` in
`frontend`, and copies `frontend/dist` into `service/src/main/resources/static`. From there the normal build puts it in
the classes of the service, and the service also finds it when it is started from the sources. Nothing has to be
installed or copied by hand. The directory is ignored by git, and `mvn clean` empties it. The Node version is `frontend.node.version` in the parent
`pom.xml`.

For backend work that does not touch the frontend, leave the frontend out of the build to save time:
```shell
mvn clean install -Dfrontend.skip=true
```
The static files already in `service/src/main/resources/static` are used as they are, if there are any.

For frontend work, run the Vite dev server in `frontend` (`npm install`, then `npm run dev`). It proxies the API calls
to the service on port 8020.


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
## GUI Wording

Texts in the GUI follow these rules, so that the same thing is called the same everywhere.

*   **Product name:** OpenID Federation Registry, from `PRODUCT_NAME` in `frontend/src/config/product.js`.
*   **Terms from OpenID Federation** are written as in the specification, also in running text: Trust Mark, Trust Mark
    Issuer, Trust Mark type, Trust Anchor, Intermediate. Trust Mark Subject and Organization follow the same pattern.
    Code identifiers such as `trustmark` are not changed.
*   **Creating:** Add, in the list button, the page heading, the browser tab title and the submit button. A saved
    object is saved with Save.
*   **Deleting:** Delete followed by the object, for example Delete Subordinate, on the button and in the confirmation
    dialog. Remove is only used for taking an item out of a field before it is saved.
*   **Rejecting:** Reject followed by the object, for example Reject Registration. Confirm buttons name the action,
    without "Yes" or "Confirm".
*   **Add buttons in list fields** say what they add, for example Add Crit.
*   **Empty lists** say what is missing and how to add it, with `EmptyState`: "No subordinates yet. Use Add Subordinate
    to add one." A search that matches nothing gives "No results for …".
*   **Modules:** Trust Anchor, Intermediate, Resolver and Trust Mark Issuer are called modules, not roles.

Copyright &copy; 2025, [SwedenConnect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
