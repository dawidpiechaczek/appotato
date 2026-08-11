import ComposeApp
import FirebaseRemoteConfig
import Foundation

/// iOS binding for the Kotlin `RemoteConfig` contract — same arrangement as `FirebaseTelemetry`:
/// the Firebase SDK is pulled in through SPM here in the Xcode project, so the binding is written
/// on this side and injected into Koin from `iOSApp.init()`.
///
/// `ComposeApp.RemoteConfig` has to be spelled out: `FirebaseRemoteConfig` exports a `RemoteConfig`
/// class of its own, and an unqualified name here is ambiguous.
///
/// Must subclass NSObject: a Swift type can only adopt a Kotlin-exported protocol through ObjC.
final class FirebaseRemoteConfigBinding: NSObject, ComposeApp.RemoteConfig {

    /// Firebase throttles anything shorter on release builds anyway; debug fetches on every call so
    /// a console change is visible while you are testing it.
    #if DEBUG
        private static let minimumFetchInterval: TimeInterval = 0
    #else
        private static let minimumFetchInterval: TimeInterval = 3600
    #endif

    private let config: FirebaseRemoteConfig.RemoteConfig

    override init() {
        config = FirebaseRemoteConfig.RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = Self.minimumFetchInterval
        config.configSettings = settings
        super.init()
    }

    func refresh(onResult: @escaping (KotlinBoolean) -> Void) {
        config.fetchAndActivate { status, _ in
            onResult(KotlinBoolean(bool: status != .error))
        }
    }

    func getString(key: String) -> String {
        // stringValue is "" for an unknown key, which is what the Kotlin contract asks for.
        config.configValue(forKey: key).stringValue
    }

    func getBoolean(key: String) -> Bool {
        config.configValue(forKey: key).boolValue
    }

    func getLong(key: String) -> Int64 {
        config.configValue(forKey: key).numberValue.int64Value
    }
}
