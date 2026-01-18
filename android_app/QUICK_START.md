# Transaction Hub Android App - Quick Start Guide

## ✅ What's Been Created

A complete Android application with:
- **Login Screen** - Authenticate with your Django backend
- **Dashboard** - View total clients, funding, balance, PnL, and your share
- **Clients List** - Browse all your clients
- **Material Design UI** - Modern, clean interface
- **Token Authentication** - Secure API access

## 🚀 Quick Setup (5 Minutes)

### Step 1: Update Server URL

**CRITICAL:** Open this file and update the server URL:

```
android_app/app/src/main/java/com/transactionhub/utils/ApiClient.kt
```

Change line 7:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:8000/"
```

**Options:**
- **Local Testing (Same WiFi):** Use your Mac's IP address
  - Find it: `ifconfig | grep "inet " | grep -v 127.0.0.1`
  - Example: `"http://192.168.1.100:8000/"`
- **Production:** Use your domain
  - Example: `"https://chip.pravoo.in/"`

### Step 2: Open in Android Studio

1. Open **Android Studio**
2. Click **"Open"** or **File → Open**
3. Navigate to: `/Users/pradyumna/chip-3/android_app`
4. Wait for Gradle sync (may take 2-3 minutes first time)

### Step 3: Start Django Server

In a terminal:
```bash
cd /Users/pradyumna/chip-3
DB_USER=pradyumna python3 manage.py runserver 0.0.0.0:8000
```

**Important:** Use `0.0.0.0:8000` instead of `127.0.0.1:8000` so your phone can connect!

### Step 4: Build APK

1. In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for build (1-2 minutes)
3. Click **"locate"** in the notification bar
4. Find `app-debug.apk` in `android_app/app/build/outputs/apk/debug/`

### Step 5: Install on Phone

**Option A: USB (Easiest)**
```bash
# Connect phone via USB, then:
adb install android_app/app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Transfer File**
1. Send `app-debug.apk` to your phone (WhatsApp, Email, AirDrop)
2. Open the file on your phone
3. Tap **Install** (allow "Unknown Sources" if prompted)

## 📱 Using the App

1. **Login:** Use your Django admin username/password
2. **Dashboard:** Swipe to see your totals
3. **Clients:** Tap bottom nav to see client list
4. **Logout:** Tap menu (3 dots) → Logout

## 🔧 Troubleshooting

**"Network error" or "Connection refused"**
- ✅ Django server running? Check terminal
- ✅ Using `0.0.0.0:8000` not `127.0.0.1:8000`?
- ✅ Phone and Mac on same WiFi?
- ✅ IP address correct in `ApiClient.kt`?

**"Invalid credentials"**
- ✅ Username/password correct?
- ✅ User exists in Django admin?

**Build fails**
- ✅ Android Studio updated?
- ✅ Internet connection? (Gradle downloads dependencies)
- ✅ Try: **File → Invalidate Caches / Restart**

**APK won't install**
- ✅ Enable "Unknown Sources" in Android Settings
- ✅ Uninstall old version first if updating

## 📂 Project Structure

```
android_app/
├── app/
│   ├── src/main/
│   │   ├── java/com/transactionhub/
│   │   │   ├── LoginActivity.kt          # Login screen
│   │   │   ├── MainActivity.kt           # Main app container
│   │   │   ├── data/
│   │   │   │   ├── api/ApiService.kt      # API endpoints
│   │   │   │   └── models/Models.kt      # Data classes
│   │   │   ├── ui/
│   │   │   │   ├── dashboard/            # Dashboard fragment
│   │   │   │   └── clients/              # Clients fragment
│   │   │   └── utils/
│   │   │       ├── ApiClient.kt          # Retrofit setup
│   │   │       └── PrefManager.kt        # SharedPreferences
│   │   └── res/
│   │       ├── layout/                   # XML layouts
│   │       └── values/                  # Strings, themes
│   └── build.gradle                      # Dependencies
└── README.md
```

## 🎯 Next Steps

Want to add more features?

1. **Accounts View** - Show all exchange accounts
2. **Transactions List** - View transaction history
3. **Pull to Refresh** - Swipe down to reload
4. **Offline Mode** - Cache data for offline use
5. **Dark Mode** - Theme support

## 📞 Need Help?

Check the main README.md in the android_app folder for detailed documentation.
