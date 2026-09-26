package com.pepperonas.brutus.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Where the newest published version comes from. Faked in tests. */
interface LatestVersionSource {
    /** The newest release tag, e.g. "v2.3.0", or null when no source answered usefully. */
    fun latestVersion(): String?
}

/**
 * Asks the product page first (its latest.json is mirrored from GitHub every 15 minutes) and GitHub's
 * public API only when the page is unreachable or answers garbage. [fetch] returns the response body
 * or null; the default does a plain GET with no identifiers attached.
 */
class ReleaseSource(
    private val fetch: (String) -> String? = ::httpGet,
) : LatestVersionSource {

    override fun latestVersion(): String? =
        fetch(SITE_URL)?.let(::versionFromSite)
            ?: fetch(GITHUB_URL)?.let(::versionFromGithub)

    companion object {
        const val SITE_URL = "https://brutus.celox.io/latest.json"
        const val GITHUB_URL = "https://api.github.com/repos/pepperonas/brutus/releases/latest"
        const val DOWNLOAD_URL = "https://brutus.celox.io/download"

        private const val TIMEOUT_MS = 10_000
        private const val MAX_BODY = 64 * 1024

        fun versionFromSite(body: String): String? = stringField(body, "version")

        fun versionFromGithub(body: String): String? = stringField(body, "tag_name")

        private fun stringField(body: String, name: String): String? = try {
            val value = JSONObject(body).opt(name)
            (value as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

        fun httpGet(url: String): String? {
            var conn: HttpURLConnection? = null
            return try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Brutus")
                }
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
                conn.inputStream.use { input ->
                    val bytes = input.readNBytesCompat(MAX_BODY)
                    String(bytes, Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            } finally {
                conn?.disconnect()
            }
        }

        private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (out.size() < max) {
                val n = read(buf, 0, minOf(buf.size, max - out.size()))
                if (n < 0) break
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }
}
