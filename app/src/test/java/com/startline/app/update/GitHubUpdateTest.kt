package com.startline.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateTest {

    @Test
    fun comparesNumericVersionParts() {
        assertTrue(GitHubUpdate.isNewer("1.0.2", "1.0.1"))
        assertTrue(GitHubUpdate.isNewer("1.1", "1.0.9"))
        assertFalse(GitHubUpdate.isNewer("1.0.1", "1.0.1"))
        assertFalse(GitHubUpdate.isNewer("1.0", "1.0.0"))
        assertEquals(0, GitHubUpdate.compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun normalizesTag() {
        assertEquals("1.0.2", GitHubUpdate.normalizeTag("v1.0.2"))
        assertEquals("1.0.2", GitHubUpdate.normalizeTag("V1.0.2-beta"))
    }

    @Test
    fun parsesApkAssetFromRelease() {
        val json = """
            {
              "tag_name": "v1.0.2",
              "draft": false,
              "prerelease": false,
              "assets": [
                {"name": "notes.txt", "url": "https://api.github.com/assets/1", "size": 10},
                {"name": "StartLine-1.0.2.apk", "url": "https://api.github.com/assets/2",
                 "browser_download_url": "https://github.com/download/StartLine-1.0.2.apk", "size": 123}
              ]
            }
        """.trimIndent()

        val release = GitHubUpdate.parseLatestRelease(json)
        assertEquals("1.0.2", release?.version)
        assertEquals("StartLine-1.0.2.apk", release?.apkName)
        assertEquals(123L, release?.apkSize)
        assertTrue(release?.apkUrl?.isNotBlank() == true)
    }

    @Test
    fun skipsPrerelease() {
        val json = """{"tag_name":"v1.0.3","prerelease":true,"assets":[]}"""
        assertNull(GitHubUpdate.parseLatestRelease(json))
    }
}
