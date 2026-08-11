import ComposeApp
import Foundation

/// Mirrors NoOpRemoteConfig in :shared:remote-config:implementation — a missing Firebase config
/// leaves every value at the zero value of its type, it does not stop the app from starting.
final class NoOpRemoteConfig: NSObject, ComposeApp.RemoteConfig {
    func refresh(onResult: @escaping (KotlinBoolean) -> Void) { onResult(KotlinBoolean(bool: false)) }
    func getString(key: String) -> String { "" }
    func getBoolean(key: String) -> Bool { false }
    func getLong(key: String) -> Int64 { 0 }
}
