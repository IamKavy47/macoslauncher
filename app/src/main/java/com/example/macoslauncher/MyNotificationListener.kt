package com.example.macoslauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pack = sbn.packageName
            val title = sbn.notification.extras?.getString("android.title") ?: ""
            val text = sbn.notification.extras?.getCharSequence("android.text")?.toString() ?: ""

            val json = JSONObject().apply {
                put("pkg", pack)
                put("title", title)
                put("text", text)
            }

            MainActivityHolder.get()?.runOnUiThread {
                try {
                    it.webView.evaluateJavascript(
                        "window.pushNotificationFromAndroid('$json')",
                        null
                    )
                } catch (e: Exception) {
                    Log.e("NotifListener", "evaljs failed", e)
                }
            }

        } catch (e: Exception) {
            Log.e("NotifListener", "error", e)
        }
    }
}
