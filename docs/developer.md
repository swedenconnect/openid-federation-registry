# Developer Guide: Entity Registry for OpenID Federation

## Building and Pushing a Docker Image

This section provides a step-by-step guide to build and push a Docker image for the project. The relevant scripts are located under `<project root>/scripts`.

### Prerequisites

Ensure you have installed the necessary tools:
- Docker
- Maven
- Bash

### Build and Push Docker Image Script

To automate the building and pushing of the Docker image, a bash script (`build-and-push.sh`) is used. Below is a description of the available options and their usage.

### Usage

```bash
./scripts/build-and-push.sh [options...]
```

#### Options
- `-i, --image`: Name of the Docker image to create (required).
- `-x, --skipTests`: Skip running tests during the `mvn clean install` phase.
- `-d, --builddir`: Directory where `Dockerfile` (and Maven POM) is placed (required).
- `-s, --buildsource`: Build Java source. If a `build-source.sh` script is available in the same directory as `build-and-push.sh`, it will be used to build the source. Otherwise, the POM file under `builddir` will be used.
- `-t, --tag`: Optional Docker tag for the image (default is `latest`).
- `-p, --push`: To push the image to the registry (registry details will be derived from the image name).
- `-a, --platform`: Optional platform/architecture parameter value to use when building the Docker image. The default is `linux/amd64,linux/arm64`.
- `-h, --help`: Prints usage information.

### Example Commands

1. **Build and push the Docker image using default multistage build and tag as `latest`:**

    ```bash
    ./scripts/build-and-push.sh -i example-repo/example-image -d /path/to/builddir -p
    ```

2. **Build without pushing, skipping tests, and specify a custom tag:**

    ```bash
    ./scripts/build-and-push.sh -i example-repo/example-image -d /path/to/builddir -x -t custom-tag
    ```

### Script Structure

The script performs the following main tasks:

1. **Parse and validate input arguments.**
2. **Determine the platform/architecture settings.**
3. **Ensure the Docker multi-host builder is configured (if doing a multi-platform build).**
4. **Build Java source if required.**
5. **Build the Docker image and optionally push it.**

### Multi-Platform Build

The script checks for the presence of the multi-host Docker builder. If it is not installed, it will prompt to run an external script `create-multi-builder.sh` from the `swedenconnect/local-environment` repository.

For more details on configuring Docker builders, you can check [Docker's official documentation](https://docs.docker.com/buildx/working-with-buildx/).

---

## Pushing to GitHub Container Registry

In addition to the above script, there is another bash script specifically designed for logging in to the GitHub Container Registry and pushing the Docker image to it.

### Usage

The script requires two environment variables to be set:
- `GITHUB_USER`: The GitHub username.
- `GITHUB_ACCESS_TOKEN`: A GitHub personal access token with appropriate permissions.

### Example Command

```bash
./scripts/build-and-push-github.sh
```

### Script Structure

The script performs the following tasks:

1. **Check for required environment variables.**
2. **Log in to the GitHub Container Registry using the provided credentials.**
3. **Build and push the Docker image using the `build-and-push.sh` script.**

For detailed information on configuring and using the GitHub Container Registry, refer to the [GitHub documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

---

For any further assistance, refer to the project's documentation or seek support from the development team.

---
Copyright &copy; 2024, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
