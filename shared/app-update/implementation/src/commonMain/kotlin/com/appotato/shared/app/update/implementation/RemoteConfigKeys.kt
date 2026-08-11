package com.appotato.shared.app.update.implementation

/**
 * Remote Config parameter names. One set for both platforms — where Android and iOS need different
 * values, that is a per-platform condition on the parameter in the Firebase console, not a second
 * key here.
 */
internal object RemoteConfigKeys {
    /** Below this version the app is blocked. Empty or unparseable disables forced updates. */
    const val MINIMUM_VERSION = "app_minimum_version"

    /** Newest published version. Below it the update is offered but dismissible. */
    const val LATEST_VERSION = "app_latest_version"

    /** Optional copy shown on the update prompt. */
    const val UPDATE_MESSAGE = "app_update_message"

    /** Optional store listing to open from the prompt. */
    const val UPDATE_URL = "app_update_url"
}
