# Project Guidelines

**Project Structure**

* **:client:composeApp**: The Presentation Layer. Contains Compose Multiplatform UI, Decompose Components, and MVI
  Stores. It consumes the Domain layer and orchestrates the UI lifecycle.
* **:client:domain**: The Domain Layer. Contains pure business logic, Repository Interfaces, and Models. **Strictly no
  UI dependencies (no Decompose, no Essenty).** It exposes data streams using standard Kotlin `StateFlow`.
* **:client:data**: The Data Layer. Contains Repository implementations, Database logic, and Network clients. All
  implementation classes must be `internal`; only the Dependency Injection factory is public.
* **:client:core**: Contains platform-specific utilities and abstractions.
* **:backend:server**: The module for the http4k server.
* **:backend:infrastructure**: Sets up the Postgres database and Keycloak Podman containers.
* **:shared:configuration**: Contains constant variables for connecting to Keycloak and Postgres. All modules use this
  as the source of truth.
* **:shared:logic**: Contains pure logic shared between both the Server and the Client (e.g., validation rules).

**Architectural Rules**

* **Domain Purity**: The Domain layer must remain framework-agnostic. Do not import Decompose, Essenty, or
  Android-specific libraries into `:client:domain`.
* **Reactive Flow**:
* **Repositories (Data/Domain)** must return `StateFlow<T>` or `Flow<T>`.
* **Components (Presentation)** must convert these Flows to `Value<T>` using `asValue(lifecycle)` for UI consumption.


* **Visibility**: In `:client:data`, all classes (Repositories, Data Sources, Services) must be `internal`. Only
  interfaces and the DI Factory should be exposed.
* **Lifecycle Management**: The Presentation layer (`:client:composeApp`) is solely responsible for managing coroutine
  scopes and lifecycles via Essenty.

**Coding Standards**

* Give variables and files explicit full names with no abbreviations.
* Do not include any code comments at all.
* If possible, keep interfaces and implementations in the same file, with the interface on top and the implementation
  below.
* Use sealed interfaces to enforce type safety and prevent invalid states.
* Always use value classes whenever possible to improve type safety.
* Reference `shared/logic/src/commonMain/kotlin/com/availelabs/availe/domain/model/ValueObjects.kt` for definitions.


* Code should only be placed in `:shared` directories if it is genuinely shared between the Client and the Server.