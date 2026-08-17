import ComposeApp
import Foundation

/// What a build with no Firebase configuration gets, mirroring `NoOpTelemetry` and
/// `NoOpRemoteConfig`. Reporting "no token" is the honest answer, and callers already handle it —
/// a real device can fail attestation for reasons that have nothing to do with configuration.
final class NoOpAttestationTokens: NSObject, AttestationTokens {
    func token(onResult: @escaping (String?) -> Void) {
        onResult(nil)
    }
}
