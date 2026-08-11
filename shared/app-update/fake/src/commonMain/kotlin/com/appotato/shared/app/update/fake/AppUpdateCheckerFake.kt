package com.appotato.shared.app.update.fake

import com.appotato.shared.app.update.api.AppUpdateChecker
import com.appotato.shared.app.update.api.AppUpdateStatus
import com.appotato.shared.app.update.api.AppVersion

/** Returns whatever [status] is set to and counts the calls. */
public class AppUpdateCheckerFake(
    override var installedVersion: AppVersion? = AppVersion(major = 1)
) : AppUpdateChecker {

    public var status: AppUpdateStatus = AppUpdateStatus.UpToDate

    public var checkCount: Int = 0
        private set

    override suspend fun check(): AppUpdateStatus {
        checkCount++
        return status
    }
}
