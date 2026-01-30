![Logo](docs/images/sweden-connect.png)

# OpenID Federation Entity Registry Service

A Spring Boot REST service implementing the OpenID Federation Entity Registry API.

---

## Table of Contents

- [About](#about)
    - [Overview](#overview)
    - [Features](#features)
- [Getting Started](#getting-started)
- [Documentation](#documentation)
- [License](#license)

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

- **Entity Management**: Create, retrieve, update, and delete federation entities
- **Policy Management**: Manage federation policies that govern entity behavior
- **Trust Mark Management**: Assign and manage trust marks for federation subjects
- **Module Management**: Handle federation modules and their configurations
- **RESTful API**: Comprehensive REST API following OpenID Federation specifications
- **OAuth2 Security**: Secure API access using OAuth2 JWT tokens
- **Audit Logging**: Comprehensive audit trail for all operations
- **Database Migrations**: Automated schema management using Flyway

---

## Getting Started

### Prerequisites

- Java SDK 21 or later
- Maven 3.6.3 or later
- Docker and Docker Compose (for local development)
- MariaDB database

### Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/swedenconnect/oidf-entity-registry.git
   cd oidf-entity-registry
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. For local development setup, see the [Developer Guide](docs/developer.md).

---

## Documentation

- **[Developer Guide](docs/developer.md)** - Setup instructions for development environment
- **[Configuration Guide](docs/configuration.md)** - Application configuration reference
- **[OAuth Scopes](docs/oauth.md)** - OAuth2 scope definitions and API access control
- **[Audit Events](docs/audit.md)** - Audit event documentation
- **[Validation Rules](docs/validation.md)** - Input validation rules and syntax

---

## License

The OIDF Entity Registry is Open Source software released under
the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

Copyright &copy; 2025, [Sweden Connect](https://www.swedenconnect.se). Licensed under version 2.0 of
the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
