package com.appotato.shared.app.update.implementation

import com.appotato.shared.app.update.api.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun `Given a full version string When parsed Then all segments are read`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("1.2.3"))
    }

    @Test
    fun `Given missing segments When parsed Then they default to zero`() {
        assertEquals(AppVersion(1, 0, 0), AppVersion.parse("1"))
        assertEquals(AppVersion(1, 2, 0), AppVersion.parse("1.2"))
    }

    @Test
    fun `Given a pre-release suffix When parsed Then it is ignored`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse("1.2.3-beta.1"))
    }

    @Test
    fun `Given surrounding whitespace When parsed Then it is trimmed`() {
        assertEquals(AppVersion(1, 2, 3), AppVersion.parse(" 1.2.3 "))
    }

    @Test
    fun `Given a malformed version When parsed Then null`() {
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("1.x.3"))
        assertNull(AppVersion.parse("1.2.3.4"))
        assertNull(AppVersion.parse("-1.2.3"))
    }

    @Test
    fun `Given two versions When compared Then segments are compared numerically`() {
        assertTrue(AppVersion(1, 9, 0) < AppVersion(1, 10, 0))
        assertTrue(AppVersion(2, 0, 0) > AppVersion(1, 99, 99))
        assertTrue(AppVersion(1, 2, 3) > AppVersion(1, 2, 2))
        assertEquals(0, AppVersion(1, 2, 3).compareTo(AppVersion(1, 2, 3)))
    }

    @Test
    fun `Given a version When printed Then all three segments are shown`() {
        assertEquals("1.2.0", AppVersion(1, 2).toString())
    }
}
