import ComposeApp
import Foundation

class RootHolder: ObservableObject {
    let lifecycle: LifecycleRegistry
    let root: IRootComponent

    init() {
        lifecycle = LifecycleRegistryKt.LifecycleRegistry()
        let context = DefaultComponentContext(lifecycle: lifecycle)
        let activity = PlatformActivity()

        let appContainer = ApplicationContainerKt.createApplicationContainer(
            instanceKeeper: context.instanceKeeper,
            activity: activity
        )

        root = RootComponent(
            componentContext: context,
            appContainer: appContainer,
            deepLinkUrl: nil
        )

        LifecycleRegistryExtKt.create(lifecycle)
    }

    deinit {
        LifecycleRegistryExtKt.destroy(lifecycle)
    }
}