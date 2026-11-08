package com.pepperonas.brutus.ui.screens

import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.MainActivity

/**
 * Ways into the app from outside — launcher shortcuts and the quick-settings tile. Each one is an
 * intent action on [MainActivity]; [HomeScreen] carries it out once and reports it handled.
 */
enum class HomeRequest(val action: String) {
    NEW_ALARM("com.pepperonas.brutus.action.NEW_ALARM"),
    TIMER("com.pepperonas.brutus.action.TIMER"),
    VACATION("com.pepperonas.brutus.action.VACATION");

    fun intent(context: Context): Intent = Intent(context, MainActivity::class.java)
        .setAction(action)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    companion object {
        fun from(intent: Intent?): HomeRequest? = entries.firstOrNull { it.action == intent?.action }
    }
}
