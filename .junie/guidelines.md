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
* **:client:uiComponents**: The Bridge Layer (Domain UI Library). Contains domain-aware UI widgets (e.g., `ListingCard`,
  `ChatBubble`, `LinkContextMenuArea`). Connects the Domain layer to the Design System.
* **:client:designSystem**: The Foundation Layer (Pure UI). Contains pure UI primitives (Atoms), Theme, Colors,
  Typography, and Resources (`strings.xml`). Strictly domain-agnostic. Never imports `domain`.
* **:client:domain**: The Domain Layer. Contains pure business logic, Repository Interfaces, and Models. **Strictly no
  UI dependencies (no Decompose, no Essenty).**
* **:client:data**: The Data Layer. Contains Repository implementations, Database logic, and Network clients. All
  implementation classes must be `internal`; only the Dependency Injection factory is public.
* **:client:core**: Contains platform-specific utilities and Shared Navigation Contracts (`DeepLinkRoute`). Acts as the
  single source of truth for URL structures.
* **:backend:server**: The module for the http4k server.
* **:backend:infrastructure**: Sets up the Postgres database and Keycloak Podman containers.
* **:shared:configuration**: Contains constant variables for connecting to Keycloak and Postgres. All modules use this
  as the source of truth.
* **:shared:logic**: Contains pure logic shared between both the Server and the Client (e.g., validation rules).

**Build & Gradle Conventions**

* **Build Logic Location**:
    * All shared Gradle conventions and build utilities live in `buildLogic/`.
    * This project uses an Included Build (`pluginManagement { includeBuild("buildLogic") }`) rather than `buildSrc` to
      improve build performance and cacheability.
    * Keep `build.gradle.kts` files focused on module-specific configuration and dependencies.
* **Convention Plugins (DRY KMP setup)**:
    * Apply convention plugins exclusively using the Version Catalog accessors defined in `gradle/libs.versions.toml`.
    * **Syntax**: Use `alias(libs.plugins.availe.kmp.android)` or `alias(libs.plugins.availe.kmp.targets)` within the
      `plugins {}` block.
* **When to use which plugin**:
    * `availe.kmp.targets`: standard KMP targets + toolchain/hierarchy.
    * `availe.kmp.android`: `availe.kmp.targets` + Android KMP library support (compile/min SDK from
      `libs.versions.toml`, Android resources enabled, JVM target).
        * Modules must still set `namespace` explicitly in `androidLibrary { namespace = "..." }`.
    * `availe.kmp.sourcesets.nonWeb`: adds a shared `nonWebMain` source set wired into `androidMain`, `jvmMain`, and
      `iosMain`.
    * `availe.kmp.sourcesets.nonAndroidNonWeb`: adds `nonAndroidMain` and `nonWebMain` to model the most common sharing
      pattern across Android, iOS, JVM, and Web.
* **iOS Framework Configuration**:
    * Use `configureIosFramework(...)` from
      `buildLogic/src/main/kotlin/com/availelabs/buildlogic/KotlinMultiplatformIosFramework.kt` to configure the iOS
      framework boilerplate.
    * Keep the `export(...)` list explicit in the final framework module (especially `:client:composeApp`).

**Architectural Rules**

* **Single Source of Truth (SSOT) Architecture**:
    * The local database is the absolute Single Source of Truth.
    * The Presentation Layer (UI/MVI Stores) must **never** consume network responses directly.
    * Network requests must act purely as fetchers that upsert data into the local database.
    * The UI must strictly observe reactive streams (`Flow`) emitted directly from the local database. This guarantees
      that background syncs, optimistic updates, and manual refreshes automatically propagate to all screens
      simultaneously without manual state synchronization.
* **Dependency Flow**: Dependencies must flow downwards. `App Shell` -> `Features` -> `Session` / `UI Components` ->
  `Design System` / `Domain`.
* **Feature Isolation**: Feature modules must not depend on each other directly. They interact via shared `Domain`
  models or by defining Navigation Interfaces that the `App Shell` implements.
* **Auth-Aware Initialization (Preventing Race Conditions)**:
    * Feature Stores and Components must never fire initial network requests blindly upon initialization.
    * Network fetch triggers must be strictly gated behind the `AuthenticationState`. The system must wait for the
      global session state to resolve to a definitive state (`LoggedIn` or `LoggedOut`, strictly ignoring `Unknown` or
      uninitialized states) before firing the initial load. This ensures the backend always receives the correct
      authorization context.
* **The Thin Screen Rule (Feature Implementation)**:
    * Feature Screens must act primarily as **Layout Orchestrators** and Glue code.
    * Complex UI elements (e.g., `MessageBubble`, `ContactRow`) that rely on Domain Models must be extracted to
      `:client:uiComponents`.
    * Features should focus on connecting State/Store to Components, not defining complex drawing logic.
* **Navigation Pattern (Consumer-Driven)**:
    * **Features** define their own requirements via interfaces (e.g., `IHomeNavigator`) located within the feature
      module.
    * **App Shell** implements these interfaces, acting as the central router.
    * **Dependency Injection** passes the App Shell implementation down to the features via the Component Context or
      constructor.
* **Modal & Overlay Management**:
    * Use Decompose **Child Slots** (`ChildSlot`) for temporary, exclusive interactions (e.g., Login Guards, Dialogs).
    * Global, non-blocking overlays (e.g., Minimized Chat) are managed by the `App Shell` and exist outside the standard
      navigation stack.
* **Platform-Adaptive Navigation (Strategy Pattern)**:
    * When navigation topologies differ significantly between platforms (e.g., Mobile uses `Dashboard/Pages` vs. Web
      uses `Home/Stack` + Browser History), use **Navigation Strategies**.
    * Split the Router logic and Initial Stack calculation into `expect/actual` implementations within the feature
      module (specifically `app-shell`).
* **Component Composition (The Factory Rule)**:
    * Intermediate components (e.g., `Dashboard`) must acts as "Dumb Containers". They must not accept dependencies
      intended for their children.
    * Use **Component Factories** (e.g., `(ComponentContext) -> IChildComponent`) to pass child creation logic down the
      tree.
    * **Context Objects**: If a factory requires many dependencies, encapsulate them in a **Context Data Class** (e.g.,
      `ChildFactoryContext`) rather than passing a long list of parameters.
    * The `App Shell` is responsible for constructing these factories and injecting the necessary dependencies (like
      Navigators).
* **Visibility Control**:
    * **Data Layer**: All implementation classes must be `internal`.
    * **Feature Layers**: Store implementations (`Executor`, `Reducer`, `Bootstrapper`) and concrete Component classes
      should be `internal` where possible. Only Component Interfaces and Factories/Constructors should be public.
* **UI Separation**:
    * `designSystem` must remain pure and never import `domain`.
    * `uiComponents` is the designated place for UI code that requires `domain` models.
* **Reactive Flow & Premature Emission Suppression**:
    * **Repositories (Data/Domain)** must return `StateFlow<T>` or `Flow<T>`.
    * **Components (Presentation)** must convert these Flows to `Value<T>` using `asValue(lifecycle)` for UI
      consumption.
    * **Flicker Prevention**: The Data Layer must actively curate its reactive streams to protect the UI from
      false-positive empty states. When performing a forced network refresh on an empty local cache (e.g., via Store5),
      the Repository must intercept and suppress the initial empty emission from the local database. The stream should
      hold the `Loading` state until the network either delivers real data or explicitly confirms the remote source is
      empty (see `ListingRepository.kt` `filterNot` implementation).
* **Lifecycle Management**: Feature Components are responsible for managing their own coroutine scopes and lifecycles
  via Essenty/Decompose.
* **Structured Concurrency & Collector Management**:
    * Background tasks (e.g., prefetching) must be launched within a `CoroutineScope` tied to the Component's lifecycle.
    * Use sequential execution for background queues to avoid overloading the system, while prioritizing active user
      actions.
    * **MVI Store Collector Management**: When a Store dynamically switches its active data stream (e.g., changing
      pagination pages or applying a new filter), the previous collector `Job` must be explicitly canceled before the
      new one is launched to prevent memory leaks and UI state thrashing.

**Interaction & UX Standards**

* **Responsive by Default**:
    * `uiComponents` must not hardcode widths; they should adapt to their container to support both mobile and desktop
      contexts.
    * Features use `WindowSizeClass` to determine the high-level layout strategy (e.g., List vs. Grid, Split-View vs.
      Stack).
* **Selectable Text**: All semantic text (Headers, Body, Error Messages) must be wrapped in `SelectionContainer` to
  ensure it can be copied by the user. Exceptions are interactive elements (Buttons, Tabs) where selection interferes
  with click actions.
* **Link Behavior**: Any UI element that navigates to a new route (Cards, Navigation Items, Icons) must use
  `LinkContextMenuArea` to support standard browser interactions (Right-click "Copy Link", Middle-click "Open in New
  Tab").
* **Web Navigation**:
    * Web targets must implement `WebNavigationOwner` to ensure the browser URL and history synchronize with the
      internal navigation state.
    * **Stack Management**: On Web, use `bringToFront` or `pushToFront` instead of `push` for top-level navigation
      items. This prevents duplicate configurations in the back stack, which causes crashes and logic errors in browser
      history management.
* **Type-Safe Routing (The Boundary Transformation Pattern)**:
    * **Strict No Strings**: Do not use raw URL strings for navigation or link generation inside feature modules.
    * **Centralized Mappers**: URL string segments (e.g., "items", "events") must be isolated in a centralized Mapper (
      e.g., `FeedTypeMapper`) within `:client:core`.
    * **Uni-Directional Parsing**: External URLs are parsed immediately into Domain Types (e.g., `ListingFeedType`) at
      the application boundary (`RootNavigationMapper`). Features receive only Type-Safe objects.
    * Use the `DeepLinkRoute` sealed interfaces defined in `:client:core` as the single source of truth for both link
      generation (`DeepLinkBuilder`) and parsing.
* **State-Driven UI**:
    * UI elements like Tabs or Lists must be derived from the Component State (e.g., Decompose `ChildPages`), not
      hardcoded in Composable lists. This ensures the UI is always in sync with the actual navigation hierarchy.
* **Window Insets Management (The Consumption Rule)**:
    * **Consume What You Use**: Any component (like `AppShell` or `Scaffold`) that places UI elements over system
      areas (e.g., Bottom Navigation Bars, Top App Bars) MUST use `Modifier.consumeWindowInsets()` to report this
      consumed space to its children.
    * **Prevent Double Padding**: Child screens (e.g., `ChatScreen`) should strictly use `WindowInsets` for their own
      padding. They must rely on the parent to have correctly consumed the insets occupied by parent-level UI. This
      prevents "floating" UI elements caused by double-counting insets.

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
    * **Type-Safe Persistence & Cache Keys (The Boundary Transformation Pattern)**: Never use manual string
      concatenation to build keys for local storage, preferences, or cache invalidation. Instead, define a strict
      `Configuration` data class that encapsulates all query parameters (e.g., Categories, Filters). Serialize this data
      class to JSON to guarantee deterministic string generation immune to delimiter collisions, and wrap the output in
      a heavily typed Value Class (e.g., `value class PreferenceKey(val value: String)`). See `ListingPreferences.kt`
      and `ListingFetchConfiguration` for reference.
* **Sharing Code**: Code should only be placed in `:shared` directories if it is genuinely shared between the Client and
  the Server.