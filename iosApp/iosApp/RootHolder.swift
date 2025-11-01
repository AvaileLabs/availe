import Foundation
import ComposeApp

class RootHolder: ObservableObject {
    let lifecycle: LifecycleRegistry
    let root: IRootComponent

    init() {
        lifecycle = LifecycleRegistryKt.LifecycleRegistry()

        root = RootComponent(
            componentContext: DefaultComponentContext(lifecycle: lifecycle)
        )

        LifecycleRegistryExtKt.create(lifecycle)
    }

    deinit {
        LifecycleRegistryExtKt.destroy(lifecycle)
    }
}