# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# Project State

This repository is a **Spring Boot starter skeleton**, not yet an implemented application.

Current contents:

- `CinemaappApplication` — the `@SpringBootApplication` entry point (no other production classes)
- `CinemaappApplicationTests` — a `contextLoads()` smoke test
- `application.properties` — contains only `spring.application.name`

`SPEC.md` exists and contains the project specification. Since this document forbids inferring business requirements and delegates them all to `SPEC.md`, there are currently no functional requirements defined. Ask the user for the specification before implementing any feature.

The directory is **not a git repository**.

---

# Build & Test Commands

Use the Maven wrapper. On Windows/PowerShell use `.\mvnw.cmd`; the examples below use the POSIX form.

```bash
./mvnw clean install          # full build incl. tests
./mvnw compile                # compile only (runs Lombok + MapStruct annotation processors)
./mvnw test                   # run all tests
./mvnw spring-boot:run        # start the app on http://localhost:8080
```

Run a single test class or method:

```bash
./mvnw test -Dtest=BookingServiceTest
./mvnw test -Dtest=BookingServiceTest#reservesAvailableSeat
./mvnw test -Dtest='*ControllerTest'
```

Surefire reports land in `target/surefire-reports/`.

MapStruct implementations are generated into `target/generated-sources/annotations/`. After changing a mapper interface, run `./mvnw compile` — a stale or missing `*MapperImpl` almost always means the annotation processor has not run.

---

# Stack Specifics

These facts come from `pom.xml` and are easy to get wrong:

- **Java 26**, **Spring Boot 4.1.0**.
- Spring Boot 4 uses **modular starters**. This project depends on `spring-boot-starter-webmvc` (not `spring-boot-starter-web`), `spring-boot-starter-data-jpa`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator`, and `spring-boot-h2console`. Add the matching module, not the Boot 3 name.
- Test dependencies are likewise modular: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-thymeleaf-test`, `spring-boot-starter-actuator-test`. Each bundles `spring-boot-starter-test`, so JUnit 5, Mockito and AssertJ are on the test classpath transitively.
- **Lombok is on the classpath** and registered as an annotation processor alongside MapStruct. It is excluded from the fat jar by the `spring-boot-maven-plugin`. Note the tension with the rules below (records for DTOs, explicit constructor injection): prefer plain records/constructors for DTOs and beans, and reach for Lombok only on JPA entities, where records are not an option.
- `spring-boot-devtools` is present, so the app restarts automatically on recompile during development.

## Boot 4.1 test APIs

Avoid the deprecated Boot 3 spellings:

- Use `@MockitoBean` / `@MockitoSpyBean` (`org.springframework.test.context.bean.override.mockito`) instead of `@MockBean` / `@SpyBean`.
- `@WebMvcTest` auto-configures `MockMvcTester`, the AssertJ-based fluent API; prefer it over raw `MockMvc` + `MockMvcRequestBuilders` in new controller tests.

## H2 persistence

The persistence rules below require the database to survive an application restart, but `application.properties` currently configures **no datasource**. Hibernate's `ddl-auto` defaults to `create-drop` for embedded databases, so the current default is an in-memory H2 that is wiped on every shutdown. Implementing persistence means adding a file-based JDBC URL (e.g. `jdbc:h2:file:./data/cinemaapp`) and an appropriate `ddl-auto` before the requirement is actually met.

The H2 console is available at `/h2-console` once a datasource is configured.

## Utility libraries

`org.apache.commons:commons-lang3` (`3.20.0`, version managed by the Spring Boot BOM) and `org.apache.commons:commons-collections4` (`4.6.0`, explicit version in `pom.xml`) are on the classpath. Prefer them over hand-rolled equivalents:

- `org.apache.commons.lang3.StringUtils` for null-safe or more expressive string operations — e.g. `isBlank`/`isNotBlank` instead of manual null-and-`isBlank()` checks, `removeEnd` instead of manual `substring` index arithmetic to strip a known suffix.
- `org.apache.commons.collections4.CollectionUtils`, `SetUtils`, `ListUtils` for null-safe collection checks and as replacements for JDK `java.util.Collections` equivalents — e.g. `CollectionUtils.isEmpty` instead of a plain `.isEmpty()` call where null-safety adds value, `SetUtils.unmodifiableSet` instead of `Collections.unmodifiableSet`.

This applies to operations on `java.util.Collection`/`Set`/`List`, not to arrays, and not to `Stream` pipelines that are already clear with lambdas — Commons Collections4's pre-Java-8 `Predicate`/`Transformer` types should not replace an existing, clear `Stream` chain.

Only reach for these utilities where they genuinely improve clarity or null-safety over the plain JDK code, not as a blanket substitution — stay consistent with "Favor simplicity over cleverness" below.

---

# AI Development Guidelines

This document defines the technical architecture, implementation rules, coding standards, and testing requirements for this project.

**Do not infer business requirements from this document.**

All functional requirements are specified in `SPEC.md`.

If a conflict exists between this document and `SPEC.md`, `SPEC.md` defines the business behavior while this document defines the technical implementation.

---

# Documentation

for documentation about the Spring framework etc., please use the context7 MCP server.

# Technology Stack

The application is implemented using the following technologies:

- Java (latest LTS supported by Spring Boot 4.1)
- Spring Boot 4.1.x
- Spring MVC
- Spring Data JPA
- Hibernate
- Thymeleaf
- H2 Database
- MapStruct
- Jakarta Validation
- Maven
- JUnit 5
- Mockito
- Apache Commons Lang3
- Apache Commons Collections4

Always use the latest APIs and best practices available for Spring Boot 4.1.

Avoid deprecated Spring APIs.

---

# Architecture

The application follows a strict layered architecture.

```
Browser
    │
@Controller
    │
@Service
    │
@Repository
    │
Database
```

Responsibilities must never overlap.

Business logic belongs exclusively to the Service layer.

---

# Dependency Rules

The allowed dependency graph is:

```
Controller
      ↓
Service
      ↓
Repository
      ↓
Database
```

The following dependencies are forbidden:

- Controller → Repository
- Controller → Entity
- View → Entity
- Repository → Controller
- Repository → Service

Only Service classes may access repositories.

---

# Controller Layer

Controllers are annotated using `@Controller`.

Controllers are responsible for:

- handling HTTP requests
- validating request parameters
- invoking services
- preparing the model
- selecting Thymeleaf views

Controllers must never:

- contain business logic
- access repositories
- create entities
- perform persistence
- contain transaction logic

Controllers communicate only with Services.

Controllers work exclusively with DTOs.

---

# Service Layer

Classes are annotated with `@Service`.

Services contain all business logic.

Responsibilities include:

- business rules
- validation
- transactions
- repository coordination
- entity/DTO conversion

Only Services may access repositories.

---

# Repository Layer

Repositories:

- use `@Repository`
- extend `JpaRepository`
- contain persistence methods only

Repositories must not contain business logic.

Custom queries should only be created when derived Spring Data queries are insufficient.

---

# Persistence

The application uses an H2 database.

Requirements:

- schema generated from entity classes
- database survives application restart
- Hibernate manages schema generation

Never manually create SQL schema files unless explicitly required.

---

# DTOs

The web layer must never expose JPA entities.

The flow is always:

```
HTTP Request

↓

DTO

↓

Service

↓

Entity

↓

Repository
```

and

```
Repository

↓

Entity

↓

Service

↓

DTO

↓

Controller

↓

View
```

Views receive DTOs only.

---

# MapStruct

MapStruct is mandatory.

Use MapStruct for:

- Entity → DTO
- DTO → Entity

Manual mapping should only be used when MapStruct cannot express the required conversion.

---

# Thymeleaf

The frontend uses Thymeleaf.

Views should:

- remain simple
- contain no business logic
- receive DTOs only
- use reusable fragments where appropriate

---

# Coding Standards

Always write production-quality code.

Requirements:

- constructor injection only
- no field injection
- no `@Autowired`
- prefer immutable objects
- use `record` for DTOs where appropriate
- use `final` for fields whenever possible
- avoid mutable shared state
- avoid static utility classes unless justified
- avoid overly long methods
- avoid duplicated code
- avoid magic numbers
- avoid unnecessary comments

---

# Clean Code

Follow Clean Code principles.

The code should be:

- readable
- maintainable
- expressive
- testable
- simple

Prefer descriptive names over comments.

Comments should explain *why*, never *what*.

---

# SOLID

Follow all SOLID principles.

Especially:

- Single Responsibility
- Dependency Inversion
- Interface Segregation

---

# Error Handling

Use exceptions only for exceptional situations.

Validate user input.

Provide meaningful validation messages.

Never swallow exceptions.

---

# Validation

Use Jakarta Validation whenever possible.

Validation annotations belong to DTOs.

Business validation belongs into Services.

---

# Transactions

Transaction boundaries belong to the Service layer.

Repositories should never define transaction logic unless required.

---

# Testing

High test coverage is mandatory.

Every implemented feature requires corresponding tests.

## Service Tests

For every public service method:

- JUnit 5
- Mockito
- mocked dependencies

Cover:

- happy path
- up to three meaningful edge cases

Verify business logic only.

---

## Repository Tests

For repositories containing custom query methods:

- @DataJpaTest
- H2 database

Verify only repository behavior.

Do not test Spring Data itself.

---

## Controller Tests

For every controller method:

- @WebMvcTest

Mock dependencies.

Verify:

- happy path
- edge cases
- model attributes
- selected view
- response status

---

## Integration Tests

Create @SpringBootTest integration tests.

One happy-path integration test per endpoint is sufficient.

Avoid mocking whenever possible.

Only mock external systems if absolutely necessary.

---

# Package Structure

Organize the project by technical layer.

Example:

```
controller
service
repository
entity
dto
mapper
config
exception
validation
```

Keep related classes together.

---

# Naming Conventions

Class names:

- singular nouns
- descriptive

Methods:

- verbs

Variables:

- descriptive
- avoid abbreviations

Boolean methods should read naturally:

```
isAvailable()
hasSeats()
canReserve()
```

---

# Code Generation Rules

Whenever implementing functionality:

1. Read the relevant section in SPEC.md.
2. Identify affected layers.
3. Create or update entities.
4. Create repositories.
5. Create DTOs.
6. Create MapStruct mappers.
7. Implement services.
8. Implement controllers.
9. Create Thymeleaf templates.
10. Create tests.
11. Ensure the project compiles.

Never skip tests.

Never generate placeholder implementations.

Never generate TODO comments.

Never leave methods unimplemented.

Generated code should be immediately executable.

---

# Preferred Spring Practices

Always prefer:

- constructor injection
- configuration properties
- immutable DTOs
- Spring Boot auto-configuration
- standard Spring MVC
- Spring Validation
- Spring Data conventions

Avoid:

- deprecated APIs
- unnecessary configuration
- reflection-based workarounds

---

# General Expectations

Generated code should look as if it were written by an experienced Spring Boot developer.

Favor simplicity over cleverness.

Prefer maintainability over optimization.

Avoid over-engineering.

When multiple solutions exist, choose the simplest solution that satisfies the requirements.

Always keep the implementation consistent with the existing codebase.
