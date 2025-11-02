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
        // Uses experimental API from kotlin-multiplatform-oidc library
        let platformAuthConfig = AuthService_iosKt.createPlatformAuthConfig(activity: activity)

        root = RootComponent(
            componentContext: DefaultComponentContext(lifecycle: lifecycle),
            codeAuthFlowFactory: codeAuthFlowFactory,
            tokenStore: tokenStore,
            platformAuthConfig: platformAuthConfig
        )

        LifecycleRegistryExtKt.create(lifecycle)
    }

    deinit {
        LifecycleRegistryExtKt.destroy(lifecycle)
    }
}