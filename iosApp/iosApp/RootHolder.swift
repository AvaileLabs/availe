import Foundation
import ComposeApp

class RootHolder: ObservableObject {
    let lifecycle: LifecycleRegistry
    let root: IRootComponent

    init() {
        lifecycle = LifecycleRegistryKt.LifecycleRegistry()

        let activity = PlatformActivity()
        let codeAuthFlowFactory = AuthFactory_iosKt.createPlatformAuthFlowFactory(activity: activity)
        let tokenStore = PlatformTokenStore_iosKt.createTokenStore(activity: activity)

        root = RootComponent(
            componentContext: DefaultComponentContext(lifecycle: lifecycle),
            codeAuthFlowFactory: codeAuthFlowFactory,
            tokenStore: tokenStore
        )

        LifecycleRegistryExtKt.create(lifecycle)
    }

    deinit {
        LifecycleRegistryExtKt.destroy(lifecycle)
    }
}