# Project Guidelines

Project structure:

* :client:composeApp is the Compose Multiplatform module
* :backend:server is the module for the http4k server
* :backend:infrastructure sets up the postgres database and the keycloak Podman containers
* :shared:configuration contains the constant variables to connect to keycloak and postgres database. All the modules
  use the
  configuration module as their source of truth.
* :shared:logic contains code which is shared between both the server and the composeApp module.

Other instructions:

* Give variables and files explicit full names with no abbreviations.
* Do not include any code comments at all.
* If possible, keep interfaces and implementations in the same file, with the interface on top of the file and
  the implementation below the interface.
* The following file is crucial as it includes definitions for many value classes:
  shared/logic/src/commonMain/kotlin/com/availelabs/availe/domain/model/ValueObjects.kt
* The shared/logic/src/commonMain/kotlin/com/availelabs/availe/domain directory is important in general.
* Again, code is only to go in the shared directory if it's shared across multiple modules.
* Always use value classes whenever possible to improve type safety.
* Use sealed interfaces to enforce type safety and prevent invalid states.