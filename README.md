MacOS WebView Launcher - Generated Project
==========================================

What this generator produced:
- Android Studio project at `MacOSLauncher/`
- Web-based launcher UI in `app/src/main/assets/index.html`
- Kotlin MainActivity with JS bridge (getInstalledApps returns icons as Base64)
- NotificationListenerService that forwards notifications to the web UI
- Bridge methods: launchApp, getInstalledApps, setWallpaper(base64), toggleBluetooth, toggleWifi (Q+ fallback to settings), setBrightness (requires user WRITE_SETTINGS), openAppSettings, sendNotification

Important runtime steps after install (do these on device):
1. Open Settings -> Apps -> Special app access -> Notification access -> Enable your launcher.
2. To allow writing brightness programmatically, grant "Modify system settings" when prompted, or manually via:
   Settings -> Apps -> Special app access -> Modify system settings -> enable for your app.
3. On Android 10+ toggling Wi-Fi directly is not allowed; the app will open the Wi-Fi panel for the user.
4. Bluetooth toggling may show a system confirmation depending on Android version.
5. If you want to publish to Play Store, follow Play Store rules for launcher apps and request only needed permissions.

Build:
- Open the project in Android Studio, let Gradle sync.
- Run on device, choose the launcher as HOME when system prompts.
- For CI (Codemagic/GitHub Actions), push the repository and configure a Gradle/Android workflow.

Security & privacy:
- Notification access is sensitive: clearly inform users why you need it.
- Don’t collect or transmit notification data externally unless you have informed consent.
