# SmartExpense Android App

Android app for automatic SMS expense tracking with strict OTP filtering.

## Features

- **Automatic SMS Detection**: Uses Android's built-in SMS API (no third-party services)
- **Strict OTP Filtering**: Blocks all OTP/verification messages
- **Transaction Only**: Only processes debit/credit SMS
- **Backend Integration**: Sends filtered SMS to your existing Spring Boot backend
- **Dashboard**: View monthly expense reports

## OTP Filtering (Strict)

The app **strictly blocks** messages containing:
- otp, OTP, verification, verify, code, CODE
- pin, PIN, password, PASSWORD, auth, AUTH
- one time, onetime, 2fa, mfa, secure, security
- expire, expiry, valid for, validity, confirm
- authentication, authenticate, login, sign in

Only processes messages containing:
- debited, credit, spent, paid, purchase, bought
- transaction, transfer, withdrawn, deposit, refund
- spent at, paid to, bought from, charged
- rs., inr, amount, balance, account

## Setup Instructions

### 1. Install Android Studio
- Download from https://developer.android.com/studio
- Install and open

### 2. Open Project
- Open Android Studio
- File → Open
- Select the `smartexpense-android` folder

### 3. Update Backend URL
Open `RetrofitClient.kt` and update:
```kotlin
private const val BASE_URL = "https://your-actual-render-url.onrender.com"
```

### 4. Build and Install
- Click Run (green play button)
- Or Build APK: Build → Build Bundle(s) / APK(s) → Build APK(s)
- Install on your Android phone

### 5. Grant Permissions
- Open the app
- Grant SMS permission when prompted
- Login with your web app credentials

### 6. Test SMS Detection
- Send a test SMS: "Rs 450 debited from account to swiggy"
- Should appear in your web app dashboard automatically
- Send an OTP SMS: "Your OTP is 123456"
- Should be blocked (not processed)

## Architecture

```
Android Phone (SMS) → SmsReceiver → OTP Filter → Backend API → Database
                    ↓
                OTP Blocked
```

## Key Files

- `SmsReceiver.kt` - SMS receiver with OTP filtering
- `MainActivity.kt` - Login and dashboard UI
- `RetrofitClient.kt` - Backend API client
- `ApiService.kt` - API endpoints

## Permissions

- `RECEIVE_SMS` - To receive SMS
- `READ_SMS` - To read SMS content
- `INTERNET` - To communicate with backend

## Security

- No third-party SMS services
- OTP messages strictly blocked
- User authentication required
- Token-based API calls

## Your Backend Remains Unchanged

Your Spring Boot backend stays exactly the same:
- Same APIs
- Same database
- Same categorization
- Web app still works

The Android app is just another client that can access SMS.
