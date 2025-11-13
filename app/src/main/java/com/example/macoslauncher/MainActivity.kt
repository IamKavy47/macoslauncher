package com.example.macoslauncher

import android.app.WallpaperManager
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/index.html")

        MainActivityHolder.set(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivityHolder.clear()
    }

    inner class AndroidBridge {
        private val gson = Gson()

        @JavascriptInterface
        fun launchApp(pkg: String) {
            try {
                val pm: PackageManager = packageManager
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    // fallback to Play Store
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(i)
                }
            } catch (e: Exception) {
                Log.e("AndroidBridge", "launchApp error", e)
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            // returns JSON array with fields: id, name, icon (base64 PNG)
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val out = ArrayList<Map<String, String>>()
            for (app in apps) {
                // Filter system apps? optional - include all but you can skip system flags:
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // skip system apps if you want: continue
                }
                val label = pm.getApplicationLabel(app).toString()
                val pkg = app.packageName
                val iconDrawable = pm.getApplicationIcon(app)
                val iconBase64 = drawableToBase64(iconDrawable)
                out.add(mapOf("id" to pkg, "name" to label, "icon" to iconBase64))
            }
            return gson.toJson(out)
        }

        private fun drawableToBase64(drawable: Drawable): String {
            val bmp = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val bytes = baos.toByteArray()
            return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        @JavascriptInterface
        fun setWallpaper(base64Image: String) {
            try {
                val data = base64Image.substringAfter("base64,")
                val bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val wm = WallpaperManager.getInstance(applicationContext)
                wm.setBitmap(bmp)
            } catch (e: Exception) {
                Log.e("AndroidBridge", "setWallpaper error", e)
            }
        }

        @JavascriptInterface
        fun toggleBluetooth(enable: Boolean) {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null) {
                    if (enable && !adapter.isEnabled) adapter.enable()
                    if (!enable && adapter.isEnabled) adapter.disable()
                } else {
                    Log.w("AndroidBridge", "No Bluetooth Adapter")
                }
            } catch (e: Exception) {
                Log.e("AndroidBridge", "toggleBluetooth error", e)
            }
        }

        @JavascriptInterface
        fun toggleWifi(enable: Boolean) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: cannot directly set Wi-Fi; open Wi-Fi panel for user
                    val panel = Intent(Settings.Panel.ACTION_WIFI)
                    panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(panel)
                } else {
                    val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = enable
                }
            } catch (e: Exception) {
                Log.e("AndroidBridge", "toggleWifi error", e)
            }
        }

        @JavascriptInterface
        fun setBrightness(value: Int) {
            try {
                // value 0-255
                if (!Settings.System.canWrite(this@MainActivity)) {
                    // can't write settings, open panel for user
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:" + packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return
                }
                val v = value.coerceIn(0, 255)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
            } catch (e: Exception) {
                Log.e("AndroidBridge", "setBrightness error", e)
            }
        }

        @JavascriptInterface
        fun openAppSettings(pkg: String) {
            try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:" + pkg)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            } catch (e: Exception) {
                Log.e("AndroidBridge", "openAppSettings error", e)
            }
        }

        @JavascriptInterface
        fun sendNotification(jsonStr: String) {
            // forwards to web UI (if needed)
            runOnUiThread {
                try {
                    webView.evaluateJavascript("window.pushNotificationFromAndroid($jsonStr)", null)
                } catch (e: Exception) {
                    Log.e("AndroidBridge", "sendNotification eval error", e)
                }
            }
        }
    }
}
