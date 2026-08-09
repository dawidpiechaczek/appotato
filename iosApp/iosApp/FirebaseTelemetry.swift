import ComposeApp
import FirebaseAnalytics
import FirebaseCrashlytics
import Foundation

/// iOS binding for the Kotlin `Telemetry` contract.
///
/// The Firebase iOS SDK is Swift/ObjC and is pulled in through SPM here in the Xcode project, so
/// the binding is written on this side and injected into Koin from `iOSApp.init()`. Kotlin never
/// sees Firebase, and nothing about Firebase leaks into shared code.
///
/// Must subclass NSObject: a Swift type can only adopt a Kotlin-exported protocol through ObjC.
final class FirebaseTelemetry: NSObject, Telemetry {

    func track(event: TelemetryEvent) {
        Analytics.logEvent(event.name, parameters: event.parameters)
    }

    func setUserId(userId: String?) {
        Analytics.setUserID(userId)
        // Crashlytics has no "unset": the empty string is how the identity is detached.
        Crashlytics.crashlytics().setUserID(userId ?? "")
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordError(error: KotlinThrowable) {
        // Crashlytics only takes NSError, so carry the Kotlin type and message across by hand —
        // otherwise every non-fatal from shared code groups into one unreadable bucket.
        let userInfo: [String: Any] = [
            NSLocalizedDescriptionKey: error.message ?? "Unknown Kotlin error",
            "kotlinType": String(describing: type(of: error))
        ]
        Crashlytics.crashlytics().record(
            error: NSError(domain: "KotlinThrowable", code: 0, userInfo: userInfo)
        )
    }
}
