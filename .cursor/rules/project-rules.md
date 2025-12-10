# Project Rules & Architectural Guidelines

This document outlines the architectural patterns, coding standards, and best practices observed in the `ms-auth` microservice. These rules must be strictly followed for all future development in this project.

## 1. Architecture: Hexagonal (Ports & Adapters)

The project follows a strict Hexagonal Architecture with three distinct layers:

### 1.1 Domain Layer (`com.gems.auth.domain`)
- **Purpose**: Contains the core business logic and rules. Independent of frameworks and external details.
- **Components**:
    - **Entities** (`domain.entities`): Rich domain models.
        - **Rule**: Entities must enforce invariants.
        - **Rule**: Use **Value Objects** for all attributes (e.g., `UserId`, `Email`, `Password`) instead of primitives.
        - **Rule**: Entities should not have public setters. State changes should happen via semantic methods or constructors.
    - **Value Objects** (`domain.values`): Immutable objects wrapping primitives.
        - **Rule**: encapsulate validation logic (e.g., email format, password strength).
    - **Constants**: Domain-specific constants.

### 1.2 Application Layer (`com.gems.auth.application`)
- **Purpose**: Orchestrates business logic by interacting with the Domain and Infrastructure layers.
- **Components**:
    - **Use Cases** (`application`): Classes implementing specific business actions.
        - **Naming**: `[Action][Resource]UseCase` (e.g., `LoginUseCase`, `RegisterUserUseCase`).
        - **Structure**: Concrete classes (not interfaces) with a single public `execute` method.
        - **Return Type**: Always return `Mono<T>` or `Flux<T>`.
    - **Gateways (Ports)** (`application.gateway`): Interfaces defining data access and external service contracts.
        - **Naming**: `[Resource]Gateway`.
        - **Rule**: Methods must use Domain Entities, not DB Entities or DTOs.
    - **Commands** (`application.command`): Input DTOs for Use Cases.
        - **Naming**: `[Action]Command`.
        - **Type**: Java Records (preferred) or immutable classes.
    - **Responses** (`application.response`): Output DTOs for Use Cases.
        - **Naming**: `[Action]Response` or `[Resource]Response`.
    - **Exceptions** (`application.exceptions`): Domain-specific runtime exceptions.

### 1.3 Infrastructure Layer (`com.gems.auth.infrastructure`)
- **Purpose**: Implements the technical details (frameworks, databases, APIs).
- **Components**:
    - **Driving Adapters** (`infrastructure.driving`): Entry points (e.g., REST Controllers).
        - **REST**: `infrastructure.driving.rest`.
        - **Naming**: `[Resource]Controller`.
        - **Rule**: Use `UserMapper` to convert Request DTOs -> Commands and Responses -> Response DTOs.
        - **Documentation**: Use OpenAPI (`@Operation`, `@ApiResponse`) annotations.
    - **Driven Adapters** (`infrastructure.driven`): Implementations of Gateways.
        - **Naming**: `[Resource][Type]Adapter` (e.g., `UserRepositoryAdapter`).
        - **Rule**: Must implement the corresponding Gateway interface.
        - **Rule**: Convert Domain Entities <-> DB Entities (`mapToEntity`, `mapToDomain`).
    - **DB Entities** (`infrastructure.driven.postgresql`): Database-specific models.
        - **Naming**: `[Resource]Entity`.
        - **Annotations**: Spring Data R2DBC (`@Table`, `@Id`, `@Column`).

## 2. Technology Stack & Patterns

- **Language**: Java 17+
- **Framework**: Spring Boot 3+ (WebFlux)
- **Reactive Programming**: Project Reactor (`Mono`, `Flux`) is mandatory for all I/O operations.
    - **Rule**: Avoid `block()` calls. Use `flatMap`, `map`, `switchIfEmpty`, `zipWhen`, etc.
- **Database**: PostgreSQL with R2DBC (Reactive).
- **Security**: Spring Security, JWT (jjwt).
- **Validation**: Jakarta Validation (`@Valid`, `@NotNull`, etc.) in Controllers.

## 3. Coding Standards

### 3.1 Naming Conventions
- **Classes**: PascalCase.
- **Methods/Variables**: camelCase.
- **Constants**: UPPER_SNAKE_CASE.
- **Tests**: `should[ExpectedBehavior]When[Condition]`.

### 3.2 Error Handling
- **Global Handler**: Use `@RestControllerAdvice` (`GlobalExceptionHandler`).
- **Custom Exceptions**: Throw specific exceptions from the Application layer (e.g., `UserNotFoundException`).
- **Error Responses**: Standardized JSON structure (timestamp, status, error, message, path).

### 3.3 Dependency Injection
- **Rule**: Use Constructor Injection for all components.
- **Rule**: Avoid `@Autowired` on fields.

## 4. Testing Guidelines

### 4.1 Unit Tests (Application Layer)
- **Frameworks**: JUnit 5, Mockito, Reactor Test (`StepVerifier`).
- **Location**: `src/test/java/com/gems/auth/application`.
- **Structure**:
    - Use `@ExtendWith(MockitoExtension.class)`.
    - Mock Gateways using `@Mock`.
    - Inject mocks into Use Case using `@InjectMocks`.
    - Use `StepVerifier` to assert reactive streams.
    - **Pattern**: `Given` (setup mocks), `When` (execute), `Then` (verify results/calls).

### 4.2 Integration Tests (Infrastructure Layer)
- **Frameworks**: `WebTestClient` for Controllers.
- **Location**: `src/test/java/com/gems/auth/infrastructure`.
- **Rule**: Test full HTTP request/response cycle including serialization and error handling.

## 5. File Organization

```
src/main/java/com/gems/auth/
├── application/           # Application Business Rules
│   ├── command/           # Input DTOs
│   ├── constants/         # App constants
│   ├── exceptions/        # Custom exceptions
│   ├── gateway/           # Ports (Interfaces)
│   ├── response/          # Output DTOs
│   └── [UseCase].java     # Business Logic
├── domain/                # Enterprise Business Rules
│   ├── entities/          # Rich Domain Objects
│   ├── values/            # Value Objects
│   └── constants/         # Domain constants
└── infrastructure/        # Frameworks & Drivers
    ├── config/            # Spring Configuration
    ├── constants/         # Infra constants
    ├── driven/            # Adapters (DB, External APIs)
    │   └── postgresql/    # DB Implementation
    └── driving/           # Entry Points
        └── rest/          # REST Controllers
```
