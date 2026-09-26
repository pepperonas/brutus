package com.pepperonas.brutus.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Open [url] in whatever handles it (browser, the PayPal app…). Returns false when no app could take
 * it, so the caller can say so instead of failing silently.
 */
fun Context.openUrl(url: String): Boolean =
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
