import ComposeApp
import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {

    init() {
        let telemetry: Telemetry = Self.configureFirebase() ? FirebaseTelemetry() : NoOpTelemetry()
        KoinIosKt.setupKoin(telemetry: telemetry)
    }

    /// Android is initialised by FirebaseInitProvider before Application.onCreate; iOS has no
    /// equivalent, so this has to run before anything touches Firebase.
    ///
    /// The environment comes from Config-<env>.xcconfig through Info.plist, so the scheme you pick
    /// in Xcode decides which Firebase project the build talks to — same idea as the Android
    /// flavor picking its own google-services.json.
    private static func configureFirebase() -> Bool {
        let environment = Bundle.main.object(forInfoDictionaryKey: "AppEnvironment") as? String ?? "prod"
        guard let path = Bundle.main.path(forResource: "GoogleService-Info-\(environment)", ofType: "plist"),
              let options = FirebaseOptions(contentsOfFile: path) else {
            assertionFailure("Missing GoogleService-Info-\(environment).plist")
            return false
        }
        FirebaseApp.configure(options: options)
        return true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
