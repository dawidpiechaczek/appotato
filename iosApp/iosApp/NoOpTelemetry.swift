import ComposeApp
import Foundation

/// Mirrors NoOpTelemetry in :shared:telemetry:implementation — a missing Firebase config disables
/// telemetry, it does not stop the app from starting.
final class NoOpTelemetry: NSObject, Telemetry {
    func track(event: TelemetryEvent) {}
    func setUserId(userId: String?) {}
    func log(message: String) {}
    func recordError(error: KotlinThrowable) {}
}
