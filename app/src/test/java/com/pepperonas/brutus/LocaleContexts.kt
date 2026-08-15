package com.pepperonas.brutus

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale

/**
 * Contexts pinned to a specific language, so a test can assert what a German
 * user and an English user actually read — independently of the locale the test
 * JVM happens to run in.
 *
 * `createConfigurationContext` resolves against the same resource tables the app
 * uses at runtime, so these really exercise `values/` vs. `values-de/`.
 */
object LocaleContexts {

    fun forLanguage(tag: String): Context {
        val base: Context = ApplicationProvider.getApplicationContext()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(config)
    }

    /** The default resource set (`values/`). */
    fun english(): Context = forLanguage("en")

    /** The translated resource set (`values-de/`). */
    fun german(): Context = forLanguage("de")
}
