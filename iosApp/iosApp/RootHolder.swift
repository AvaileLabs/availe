import Foundation
import ComposeApp

class RootHolder : ObservableObject {
    let lifecycle: LifecycleRegistry
    let root: IRootComponent
}
