package com.appotato.shared.app.update.implementation

import com.appotato.shared.app.update.api.AppUpdateStatus
import com.appotato.shared.app.update.api.AppVersion
import com.appotato.shared.remote.config.fake.RemoteConfigFake
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteConfigAppUpdateCheckerTest {

    private val remoteConfig = RemoteConfigFake()

    private fun checker(installedVersion: String = "1.2.0") =
        RemoteConfigAppUpdateChecker(remoteConfig, installedVersion)

    @Test
    fun `Given installed version below minimum When check Then update is required`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.3.0")

        val status = checker().check()

        assertEquals(AppUpdateStatus.Required(AppVersion(1, 3, 0)), status)
    }

    @Test
    fun `Given installed version equal to minimum When check Then up to date`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.2.0")

        assertEquals(AppUpdateStatus.UpToDate, checker().check())
    }

    @Test
    fun `Given a newer version published When check Then update is available`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.0.0")
        remoteConfig.set(RemoteConfigKeys.LATEST_VERSION, "1.4.1")

        assertEquals(AppUpdateStatus.Available(AppVersion(1, 4, 1)), checker().check())
    }

    @Test
    fun `Given both thresholds passed When check Then required wins over available`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "2.0.0")
        remoteConfig.set(RemoteConfigKeys.LATEST_VERSION, "2.1.0")

        assertEquals(AppUpdateStatus.Required(AppVersion(2, 0, 0)), checker().check())
    }

    @Test
    fun `Given message and url published When check Then they are carried into the status`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.3.0")
        remoteConfig.set(RemoteConfigKeys.UPDATE_MESSAGE, "Time to update")
        remoteConfig.set(RemoteConfigKeys.UPDATE_URL, "https://example.com/store")

        val status = checker().check()

        assertEquals(
            AppUpdateStatus.Required(AppVersion(1, 3, 0), "Time to update", "https://example.com/store"),
            status
        )
    }

    @Test
    fun `Given blank message and url When check Then they are null`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.3.0")
        remoteConfig.set(RemoteConfigKeys.UPDATE_MESSAGE, "   ")

        val status = checker().check() as AppUpdateStatus.Update

        assertEquals(null, status.message)
        assertEquals(null, status.storeUrl)
    }

    @Test
    fun `Given no values published When check Then up to date`() = runTest {
        assertEquals(AppUpdateStatus.UpToDate, checker().check())
    }

    @Test
    fun `Given an unparseable minimum version When check Then up to date`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "latest")

        assertEquals(AppUpdateStatus.UpToDate, checker().check())
    }

    @Test
    fun `Given an unreadable installed version When check Then up to date`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "9.9.9")

        assertEquals(AppUpdateStatus.UpToDate, checker(installedVersion = "").check())
    }

    @Test
    fun `Given a double digit minor version When check Then it is compared numerically`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.9.0")

        assertEquals(AppUpdateStatus.UpToDate, checker(installedVersion = "1.10.0").check())
    }

    @Test
    fun `Given a failing fetch When check Then the cached values still decide`() = runTest {
        remoteConfig.set(RemoteConfigKeys.MINIMUM_VERSION, "1.3.0")
        remoteConfig.setOnRefresh(RemoteConfigKeys.MINIMUM_VERSION, "1.0.0")
        remoteConfig.refreshSucceeds = false

        assertEquals(AppUpdateStatus.Required(AppVersion(1, 3, 0)), checker().check())
    }

    @Test
    fun `Given check is called When it runs Then values are refreshed first`() = runTest {
        remoteConfig.setOnRefresh(RemoteConfigKeys.MINIMUM_VERSION, "1.3.0")

        val status = checker().check()

        assertEquals(1, remoteConfig.refreshCount)
        assertEquals(AppUpdateStatus.Required(AppVersion(1, 3, 0)), status)
    }

    @Test
    fun `Given a version name from the platform When the checker is built Then it is exposed`() {
        assertEquals(AppVersion(1, 2, 0), checker().installedVersion)
    }
}
