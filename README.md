# Glass List — Android app source

Offline Android phone-glass model and stock manager built from the supplied `glass_list.json`.

## Features
- Search model, brand, or group
- Filter by brand and group
- Copy a model name
- Save/remove favourites (stored locally)
- Add custom brand, group and phone models
- Set starting stock when adding a model
- Increase/decrease stock or set an exact quantity
- Quantity `0` is shown as **SOLD OUT**; the header shows the count of sold-out models
- Custom models, stock quantities and favourites persist locally on the device
- Works offline; no network permission required

## Build APK
1. Install Android Studio and Android SDK Platform 35.
2. Open this `GlassList` folder in Android Studio.
3. Allow Gradle sync and install any requested SDK components.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Package: `com.glasslist.app` · Version: `1.1` · Minimum Android: 6.0 (API 23)

The supplied source data is retained in `app/src/main/assets/glass_list.json`. Custom additions and stock are saved in local app preferences on the phone.
