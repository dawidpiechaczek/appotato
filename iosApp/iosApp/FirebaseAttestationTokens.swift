import ComposeApp
import FirebaseAppCheck
import Foundation

/// iOS binding for the Kotlin `AttestationTokens` contract — same arrangement as `FirebaseTelemetry`
/// and `FirebaseRemoteConfigBinding`: the Firebase SDK is pulled in through SPM here in the Xcode
/// project, so the binding is written on this side and injected into Koin from `iOSApp.init()`.
///
/// Must subclass NSObject: a Swift type can only adopt a Kotlin-exported protocol through ObjC.
final class FirebaseAttestationTokens: NSObject, AttestationTokens {

    override init() {
        // Parity with the Android binding: fetch a token in the background now and keep it fresh,
        // so the first request that needs one is not also waiting on App Attest.
        AppCheck.appCheck().isTokenAutoRefreshEnabled = true
        super.init()
    }

    /// `limitedUse: false` reuses the cached token until it is close to expiry, which is what we
    /// want — a fresh App Attest assertion on every suggestion request would be a round trip per
    /// request for no extra safety.
    func token(onResult: @escaping (String?) -> Void) {
        AppCheck.appCheck().token(forcingRefresh: false) { token, _ in
            // The contract says this never throws and every caller already handles "no token", so
            // a failure is reported as nil rather than propagated.
            onResult(token?.token)
        }
    }
}
