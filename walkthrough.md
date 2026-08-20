# Walkthrough: SMS Expense Tracker (Native Android)

We have built **SMS Expense Tracker**, a native Android app in Kotlin and Jetpack Compose (Material 3) with a **100% on-device privacy guarantee** that reads incoming and historical SMS messages, extracts structured transaction details, and generates consolidated daily, weekly, and monthly payment reports.

---

## 🔒 Privacy & Architecture Overview

```mermaid
flowchart TD
    SMS[Incoming SMS or Inbox History] --> Det[OtpPromoDetector / ExcludedSender Filter]
    Det -- Valid Transaction SMS --> Engine[SmsParsingEngine]
    Engine --> Rules[RuleRegistry: HDFC, ICICI, SBI, Axis, Kotak, PNB, BOB, IDFC, YES, UPI Apps, GenericBank]
    Rules --> Result[ParseResult: Success / LowConfidence / Ignored]
    Result --> Dedupe[FuzzyDeduplicator (2-min window check)]
    Dedupe --> DB[(Room Encrypted Local SQLite DB)]
    DB --> UI[Jetpack Compose Dashboard & Reports UI]
    DB --> Export[Offline CSV Export via SAF]
```

- **Zero Internet Permissions**: `AndroidManifest.xml` does not declare `android.permission.INTERNET`. No network calls can occur.
- **Local SQLite DB**: Room 2.6.1 with composite indices on `timestamp`, `category`, `paymentMode`, and `referenceId`.
- **Fuzzy Deduplication**: Deduplicates multi-alerts (e.g. Bank SMS + UPI app notification) within a 2-minute window.
- **Strict Credit/Debit Separation**: Daily report cards present credits received today in an emerald green container that is never netted against expenses.

---

## 🧩 Components Built

### 1. Data & Persistence Layer
- [`Transaction.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/model/Transaction.kt): Room entity with amount, type (`DEBIT`/`CREDIT`), bank, mode (`UPI`, `DEBIT_CARD`, `CREDIT_CARD`, `NET_BANKING`, `NEFT_IMPS`, `ATM_WITHDRAWAL`), category, merchant/payee, last 4 account digits, reference ID, confidence score, and timestamp.
- [`CategoryMapping.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/model/CategoryMapping.kt): User-defined keyword-to-category mapping overrides.
- [`ExcludedSender.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/model/ExcludedSender.kt): Privacy deny list for personal senders.
- [`TransactionDao.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/local/TransactionDao.kt) & [`AppDatabase.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/local/AppDatabase.kt): Room DAOs and database configuration.
- [`TransactionRepository.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/repository/TransactionRepository.kt) & [`SettingsRepository.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/data/repository/SettingsRepository.kt): Repository orchestration with local CSV export.

### 2. SMS Rule-Based Parsing Engine
- [`BankSmsRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/BankSmsRule.kt) & [`RuleRegistry.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/RuleRegistry.kt): Pluggable rule catalog.
- [`HdfcBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/HdfcBankRule.kt), [`IciciBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/IciciBankRule.kt), [`SbiBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/SbiBankRule.kt), [`AxisBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/AxisBankRule.kt), [`KotakBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/KotakBankRule.kt), [`PnbBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/PnbBankRule.kt), [`BobBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/BobBankRule.kt), [`IdfcFirstBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/IdfcFirstBankRule.kt), [`YesBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/YesBankRule.kt), [`UpiAppsRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/UpiAppsRule.kt), [`GenericBankRule.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/rules/GenericBankRule.kt).
- [`CategoryClassifier.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/CategoryClassifier.kt): Standard categories (Food & Dining, Groceries, Shopping, Bills & Utilities, Transport, Entertainment, Transfers, Health & Medical, Investments, Others) with merchant matching.
- [`OtpPromoDetector.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/OtpPromoDetector.kt): High-accuracy exclusion for OTPs and marketing promos.
- [`FuzzyDeduplicator.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/parser/FuzzyDeduplicator.kt): Multi-message deduplication.

### 3. Background Services & Workers
- [`HistoricalSmsScanWorker.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/service/HistoricalSmsScanWorker.kt): `CoroutineWorker` querying `content://sms/inbox` with chunked progress reporting.
- [`SmsBroadcastReceiver.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/service/SmsBroadcastReceiver.kt): Catches live incoming `SMS_RECEIVED` events.
- [`NotificationHelper.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/service/NotificationHelper.kt): Local Android notifications for parsed transactions.

### 4. UI Layer (Jetpack Compose Material 3)
- [`HomeScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/home/HomeScreen.kt) & [`HomeViewModel.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/home/HomeViewModel.kt): Hero Spend Card, separate credits card, Donut Chart, and today's transactions.
- [`ReportsScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/reports/ReportsScreen.kt) & [`ReportsViewModel.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/reports/ReportsViewModel.kt): Daily, weekly, and monthly reports with interactive Donut chart, 7-day trend bar chart, and breakdown tabs.
- [`TransactionDetailScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/details/TransactionDetailScreen.kt): View/edit merchant & category, notes, and raw SMS inspection with copy action.
- [`FlaggedReviewScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/review/FlaggedReviewScreen.kt) & [`FlaggedReviewViewModel.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/review/FlaggedReviewViewModel.kt): Low-confidence SMS parser verification with category keyword learning.
- [`SettingsScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/settings/SettingsScreen.kt) & [`SettingsViewModel.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/settings/SettingsViewModel.kt): Excluded senders deny list, custom category mapping rules, offline CSV export via SAF, and inbox re-scan trigger.
- [`PermissionsOnboardingScreen.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/onboarding/PermissionsOnboardingScreen.kt) & [`RationaleDialog.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/components/RationaleDialog.kt): Honest privacy disclosure sheet.
- [`AppNavigation.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/ui/navigation/AppNavigation.kt) & [`MainActivity.kt`](file:///WD-1/Antigravity/expenseApp/app/src/main/java/com/example/smsexpensetracker/MainActivity.kt): Navigation bar wiring and runtime permission request flow.

---

## 🧪 Verification & Build Results

### Automated Unit Tests
Executed `./gradlew testDebugUnitTest`:
```
BankSmsParserTest > testHdfcBankDebitSms PASSED
BankSmsParserTest > testHdfcBankCardDebitSms PASSED
BankSmsParserTest > testIciciBankDebitSms PASSED
BankSmsParserTest > testIciciBankCardDebitSms PASSED
BankSmsParserTest > testSbiBankDebitSms PASSED
BankSmsParserTest > testSbiCreditCardDebitSms PASSED
BankSmsParserTest > testAxisBankDebitSms PASSED
BankSmsParserTest > testKotakBankDebitSms PASSED
BankSmsParserTest > testPnbBankDebitSms PASSED
BankSmsParserTest > testBankOfBarodaDebitSms PASSED
BankSmsParserTest > testIdfcFirstBankDebitSms PASSED
BankSmsParserTest > testYesBankDebitSms PASSED
BankSmsParserTest > testPhonePeDebitSms PASSED
BankSmsParserTest > testCreditTransactionSms PASSED
BankSmsParserTest > testOtpMessageIsIgnored PASSED
BankSmsParserTest > testPromotionalMessageIsIgnored PASSED
BankSmsParserTest > testExcludedSenderIsIgnored PASSED
BankSmsParserTest > testFuzzyDeduplication PASSED
BankSmsParserTest > testGooglePayDebitSms PASSED
BankSmsParserTest > testPaytmDebitSms PASSED
BankSmsParserTest > testGenericBankFallbackDebitSms PASSED
BankSmsParserTest > testCustomCategoryUserMappingOverride PASSED
BankSmsParserTest > testInvestmentCategoryDetection PASSED

25 tests completed, 0 failed
BUILD SUCCESSFUL
```

### Full Application Compilation & KSP Room Generation
Executed `./gradlew clean assembleDebug testDebugUnitTest`:
```
BUILD SUCCESSFUL in 11s
45 actionable tasks: 27 executed, 18 from cache
Generated debug APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 Android 16 / Motorola Launch Crash Root Cause & Fix

### 1. Root Cause
1. **Missing Room Implementation (`AppDatabase_Impl`)**:
   - The initial build lacked KSP configuration for Room. Because Room relies on compile-time annotation processing, `AppDatabase_Impl` and `TransactionDao_Impl` were never generated.
   - On cold start, `MainActivity` and `HomeViewModel` attempted to initialize `AppDatabase.getDatabase(context)` via reflection, throwing `java.lang.RuntimeException: cannot find implementation for AppDatabase. AppDatabase_Impl does not exist` and immediately terminating the process.
2. **Invalid `windowSoftInputMode` on `<application>`**:
   - In `AndroidManifest.xml`, `android:windowSoftInputMode="adjustResize"` was declared on `<application>` instead of `<activity>`. On Android 16's strict manifest parser, invalid application attributes trigger warnings/crashes.
3. **Privileged Permission on Third-Party Receiver**:
   - `SmsBroadcastReceiver` had `android:permission="android.permission.BROADCAST_SMS"`. `BROADCAST_SMS` is a signature-only system permission. On modern Android versions (Android 14/15/16), third-party apps cannot hold or enforce `BROADCAST_SMS` without encountering security exceptions.
4. **Early `WorkManager.getInstance()` in ViewModel Constructors**:
   - Eagerly calling `WorkManager.getInstance(application)` in `HomeViewModel` during cold start could race with `androidx.startup.InitializationProvider`.

### 2. Resolution Applied
- **Configured Kotlin Symbol Processing (KSP)** with pure Kotlin code generation (`room.generateKotlin = true`) and integrated `ksp(libs.androidx.room.compiler)`.
- **Verified Generated Classes**: Verified generated `AppDatabase_Impl.kt`, `TransactionDao_Impl.kt`, `CategoryMappingDao_Impl.kt`, and `ExcludedSenderDao_Impl.kt` are properly compiled into the DEX files of `app-debug.apk`.
- **Fixed `AndroidManifest.xml`**: Moved `windowSoftInputMode="adjustResize"` into `<activity>` and cleaned up the `SmsBroadcastReceiver` declaration.
- **Lazy Initialization**: Switched `workManager` to `by lazy { WorkManager.getInstance(application) }` and added defensive error handling in `NotificationHelper.createNotificationChannel()`.
- **Verification**: Clean build completed successfully (`BUILD SUCCESSFUL in 11s`), and all 25 unit tests passed.

