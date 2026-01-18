# Transaction Hub Android App

Complete Android application for Transaction Hub built with Kotlin and Retrofit.

## Setup Instructions

### 1. Install Dependencies

First, install Django REST Framework in your Django project:

```bash
cd /Users/pradyumna/chip-3
pip install djangorestframework
python manage.py migrate  # Create token tables
```

### 2. Configure Server URL

**IMPORTANT:** Update the server URL in the Android app:

1. Open `android_app/app/src/main/java/com/transactionhub/utils/ApiClient.kt`
2. Replace `YOUR_SERVER_IP` with your actual server IP or domain
3. For local testing on same network: Use your Mac's IP (e.g., `192.168.1.100`)
4. For production: Use your deployed domain (e.g., `chip.pravoo.in`)

### 3. Open in Android Studio

1. Open Android Studio
2. Click "Open an Existing Project"
3. Navigate to `/Users/pradyumna/chip-3/android_app`
4. Wait for Gradle sync to complete

### 4. Build APK

1. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete
3. Click "locate" in the notification to find `app-debug.apk`

### 5. Install on Your Phone

**Option A: USB**
- Connect phone via USB
- Enable USB Debugging in Developer Options
- Drag `app-debug.apk` to phone or use `adb install app-debug.apk`

**Option B: Transfer File**
- Send `app-debug.apk` via WhatsApp/Email
- Open on phone and install (allow "Unknown Sources" if prompted)

## Features

- ✅ Login with username/password
- ✅ Dashboard with key metrics
- ✅ Client list view
- ✅ Token-based authentication
- ✅ Material Design UI

## API Endpoints Used

- `POST /api/login/` - User authentication
- `GET /api/mobile-dashboard/` - Dashboard summary
- `GET /api/clients/` - List all clients
- `GET /api/accounts/` - List all accounts
- `GET /api/transactions/` - Transaction history

## Troubleshooting

**"Network error" or "Connection refused"**
- Make sure Django server is running
- Check that phone and computer are on same WiFi network
- Verify IP address in `ApiClient.kt` is correct
- For production, ensure HTTPS is configured

**"Invalid credentials"**
- Verify username/password are correct
- Check Django admin to ensure user exists

**Build errors**
- Make sure Android Studio is updated
- Sync Gradle: **File** → **Sync Project with Gradle Files**
- Clean project: **Build** → **Clean Project**

## Next Steps

To add more features:
1. Add more fragments (Accounts, Transactions, Reports)
2. Implement create/edit screens
3. Add pull-to-refresh
4. Add offline caching
5. Implement push notifications
