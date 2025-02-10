&nbsp;
<p align="center">
    <img src="https://github.com/mosparo/mosparo/blob/master/assets/images/mosparo-logo.svg?raw=true" alt="mosparo logo contains a bird with the name Mo and the mosparo text"/>
</p>

<h1 align="center">
    Java API Client
</h1>
<p align="center">
    This library offers the API client to communicate with mosparo to verify a submission.
</p>

-----

## Description

With this JAVA library you can connect to a mosparo installation and verify the submitted data.

## Installation

Include the mosparo into your project. E.g. for Maven users:

```xml
<dependency>
    <groupId>io.mosparo</groupId>
    <artifactId>java-api-client</artifactId>
    <version>${mosparo.version}</version>
</dependency>
```

## Usage

1. Instantiate a mosparo Client

```java
MosparoClient client = new MosparoDefaultClient("https://<url>", "<publicKey>", "<privateKey>");
```

2. Verify a form submission using the `verifySubmission` method.

Provide the `Set` of required fields for your form verification. The set of required fields for form verification includes
all user input fields regardless of whether they are marked as mandatory in the web form.

```java
try {
    VerificationResult result = client.verifySubmission(formData, Set.of("firstname", "lastname"));
} catch (IOException e) {
    // Handle communication failure
} catch (MosparoException e) {
    // Handle verification failure
}
```

Where formData is a simple `java.util.Map<String, Object>` of the data received from the frontend. For example, it would
look like:

```java
Map<String, Object> formData = Map.of(
        "lastname", "Example",
        "firstname", "John",
        "_mosparo_submitToken", "submitToken",
        "_mosparo_validationToken", "validationToken");
```

## Acknowledgements

Gratitude is extended to _Jakobus Schürz_ for his work on https://git.schuerz.at/jakob/keycloak-mosparo. This project
provided valuable insights and motivation during the development of this independent initiative. While no code or direct
contributions from his project are included, the inspiration it offered is greatly appreciated.

# Development

## Versions

The project is configured to use [Maven CI friendly](https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html)
versioning.
To specify a non-default version, for example, `0.0.1-SNAPSHOT`, use:
```shell
mvn package -Drevision="0.0.1-SNAPSHOT"
```

## Tests

The project contains two types of tests.

### Unit tests

These tests are named according to the usual Maven convention `*Test`.
They use Wiremock to mock responses from mosparo server.

### Integration tests

These tests are named according to the Maven convention `*IT`.
They use testcontainers to create a docker compose environment with:

- mosparo server
- a database for mosparo server
- selenium browser
- a website with a simple form

They allow to test the library against a real mosparo server.

## GitHub Actions Workflows

### Build

The build workflow compiles the project and runs the tests with surefire (unit tests) and failsafe (integration tests).

## Release

The release workflow is triggered by a GitHub release. It expects a tagName starting with a "v", it will cut the first
character to create the maven release version.
This workflow expects secrets:

- SONATYPE_USERNAME
- SONATYPE_TOKEN
- GPG_PRIVATE_KEY

The secret SONATYPE_* are used to deploy the released artifacts to Maven Central Repository.
See https://central.sonatype.org/register/central-portal/ for details.

The secret GPG_PRIVATE_KEY is used to sign the generated artifacts.
See https://central.sonatype.org/publish/requirements/gpg/ for details.
