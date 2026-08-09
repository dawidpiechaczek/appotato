import ComposeApp
import FirebaseCore
import SwiftUI

@main
struct iOSApp: App {

    init() {
        // Android is initialised by FirebaseInitProvider before Application.onCreate; iOS has no
        // equivalent, so this has to run before anything touches Firebase.
        FirebaseApp.configure()
        KoinIosKt.setupKoin(telemetry: FirebaseTelemetry())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
