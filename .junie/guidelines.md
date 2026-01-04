# Project Guidelines

**Project Structure**

* **:client:composeApp**: The Root Assembler. Contains platform entry points (`MainActivity`, `App.kt`) and Dependency
  Injection wiring. Strictly no business logic.
* **:client:features:app-shell**: The UI Orchestrator and Navigation Root. Manages the global navigation tree,
  Dashboard, and high-level layout logic. Acts as the implementation provider for feature navigation interfaces.
* **:client:features:session**: Global Session State. Manages shared authentication status and user session data to
  prevent circular dependencies between features.
* **:client:features:**: Vertical Feature Slices (e.g., `auth`, `home`, `chat`, `profile`). Encapsulated modules
  containing Screens, MVI Stores, and Decompose Components. Each feature defines its own Navigation Contract.
* **:client:uiComponents**: The Bridge Layer. Contains domain-aware UI widgets (e.g., `ListingCard`) and shared UI
  utilities. Connects the Domain layer to the Design System.
* **:client:designSystem**: The Foundation Layer. Contains pure UI primitives (Atoms), Theme, Colors, Typography, and
  Resources (`strings.xml`). Strictly domain-agnostic.
* **:client:domain**: The Domain Layer. Contains pure business logic, Repository Interfaces, and Models. **Strictly no
  UI dependencies (no Decompose, no Essenty).**
* **:client:data**: The Data Layer. Contains Repository implementations, Database logic, and Network clients. All
  implementation classes must be `internal`; only the Dependency Injection factory is public.
* **:client:core**: Contains platform-specific utilities and abstractions.
* **:backend:server**: The module for the http4k server.
* **:backend:infrastructure**: Sets up the Postgres database and Keycloak Podman containers.
* **:shared:configuration**: Contains constant variables for connecting to Keycloak and Postgres. All modules use this
  as the source of truth.
* **:shared:logic**: Contains pure logic shared between both the Server and the Client (e.g., validation rules).

**Architectural Rules**

* **Dependency Flow**: Dependencies must flow downwards. `App Shell` -> `Features` -> `Session` / `UI Components` ->
  `Design System` / `Domain`.
* **Feature Isolation**: Feature modules must not depend on each other directly. They interact via shared `Domain`
  models or by defining Navigation Interfaces that the `App Shell` implements.
* **Navigation Pattern (Consumer-Driven)**:
    * **Features** define their own requirements via interfaces (e.g., `IHomeNavigator`) located within the feature
      module.
    * **App Shell** implements these interfaces, acting as the central router.
    * **Dependency Injection** passes the App Shell implementation down to the features via the Component Context or
      constructor.
* **Component Composition (The Factory Rule)**:
    * Intermediate components (e.g., `Dashboard`) must acts as "Dumb Containers". They must not accept dependencies
      intended for their children.
    * Use **Component Factories** (e.g., `(ComponentContext) -> IChildComponent`) to pass child creation logic down
      the tree.
    * The `App Shell` is responsible for constructing these factories and injecting the necessary dependencies (like
      Navigators).
* **Visibility Control**:
    * **Data Layer**: All implementation classes must be `internal`.
    * **Feature Layers**: Store implementations (`Executor`, `Reducer`, `Bootstrapper`) and concrete Component classes
      should be `internal` where possible. Only Component Interfaces and Factories/Constructors should be public.
* **UI Separation**:
    * `designSystem` must remain pure and never import `domain`.
    * `uiComponents` is the designated place for UI code that requires `domain` models.
* **Reactive Flow**:
    * **Repositories (Data/Domain)** must return `StateFlow<T>` or `Flow<T>`.
    * **Components (Presentation)** must convert these Flows to `Value<T>` using `asValue(lifecycle)` for UI
      consumption.
* **Lifecycle Management**: Feature Components are responsible for managing their own coroutine scopes and lifecycles
  via Essenty/Decompose.

**Coding Standards**

* **Naming Conventions**:
    * Use **C# Style** for interfaces (Prefix with `I`, e.g., `IListingRepository`, `IHomeNavigator`,
      `IProfileComponent`).
    * Give variables and files explicit full names with no abbreviations.
* **Comments**: Do not include any code comments at all.
* **File Structure**:
    * Keep interfaces and implementations in the same file when possible, with the interface on top and the
      implementation below.
    * **Exception**: Large, complex components (like `AppShell`) should be split into distinct packages by
      responsibility (e.g., `component`, `router`, `model`, `ui`) to ensure maintainability and separation of concerns.
* **Type Safety**:
    * Use sealed interfaces to enforce type safety and prevent invalid states.
    * Always use value classes whenever possible to improve type safety.
    * Reference `shared/logic/src/commonMain/kotlin/com/availelabs/availe/domain/model/ValueObjects.kt` for definitions.
* **Sharing Code**: Code should only be placed in `:shared` directories if it is genuinely shared between the Client and
  the Server.