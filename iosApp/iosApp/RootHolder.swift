import Foundation
import ComposeApp

class RootHolder: ObservableObject {
    let lifecycle: LifecycleRegistry
    let root: IRootComponent

    init() {
        lifecycle = LifecycleRegistryKt.LifecycleRegistry()
        let context = DefaultComponentContext(lifecycle: lifecycle)
        let activity = PlatformActivity()

        let appContainer = ApplicationContainerKt.createApplicationContainer(
            lifecycle: lifecycle,
            activity: activity
        )

        root = RootComponent(
            componentContext: context,
            appContainer: appContainer
        )

        LifecycleRegistryExtKt.create(lifecycle)
    }

    deinit {
        LifecycleRegistryExtKt.destroy(lifecycle)
    }
}