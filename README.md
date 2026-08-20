# SMS Expense Tracker 📱💸

A native, 100% on-device Android expense tracking app built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, and **WorkManager**.

The app reads incoming and historical SMS payment confirmation messages (from banks, UPI apps, credit cards, and digital wallets), extracts structured transaction metadata, automatically categorizes expenses, and generates real-time consolidated financial reports — with a **zero-cloud, 100% on-device privacy guarantee**.

---

## 🔒 Privacy & Security Architecture

1. **Zero Internet Permission**:
   - The app's `AndroidManifest.xml` explicitly **omits** `android.permission.INTERNET`.
   - It is physically impossible for SMS content, extracted financial data, or user metadata to leave the device or reach external servers.
2. **On-Device Transparent Regex Parsing**:
   - SMS processing is executed entirely in-memory using deterministic regular expressions and state-machine classifiers.
3. **Local Persistence**:
   - All transactions, category keyword rules, and excluded senders are stored locally in an encrypted Room SQLite database.
4. **Offline SAF Data Export**:
   - Users can export their complete transaction history to a CSV file anytime via Android Storage Access Framework (SAF) completely offline.
5. **Sender Exclusions (Privacy Deny List)**:
   - Users can exclude specific personal contact numbers or custom sender headers (e.g. `AX-PERSONAL`) from being read or processed.

---

## 🚀 Key Features

### 1. Multi-Bank & UPI Regex Parsing Engine
Pluggable rule architecture supporting major Indian financial institutions and payment apps:
- **Banks**: HDFC Bank, ICICI Bank, State Bank of India (SBI), Axis Bank, Kotak Mahindra Bank, Punjab National Bank (PNB), Bank of Baroda (BOB), IDFC FIRST Bank, YES Bank, Canara Bank, Union Bank, IndusInd Bank, Federal Bank, RBL Bank, and more.
- **UPI & Payment Apps**: PhonePe, Google Pay, Paytm, CRED, BHIM.
- **Payment Modes**: UPI, Debit Card, Credit Card, NetBanking, NEFT/IMPS, ATM Cash Withdrawals.
- **Smart Filters**: Automatic rejection of OTPs, 2FA codes, loan offers, pre-approved marketing SMS, and non-transactional alerts.
- **Fuzzy Deduplication**: Intelligent $\pm 2\text{ minute}$ window deduplication checking amounts, reference IDs, and timestamps to eliminate duplicate alerts from bank + UPI app for the same transaction.

### 2. Live & Historical SMS Ingestion
- **Real-Time `BroadcastReceiver`**: Listens for incoming `SMS_RECEIVED` events, parses transactions instantaneously, and posts local Android notifications.
- **WorkManager Historical Scanner**: Paginated background inbox scanner (`HistoricalSmsScanWorker`) with live progress reporting.

### 3. Material 3 Jetpack Compose UI
- **Home Dashboard**:
  - **Hero Spend Card**: Today's total spend with yesterday comparison metrics.
  - **Dedicated Credits Card**: Green highlight card keeping credits strictly separate (never netted against expenses).
  - **Category Donut Chart**: Native Compose Canvas Donut chart with touch slice percentage badges.
  - **Today's Transactions**: Chronological list with category icons, banks, modes, and amounts.
- **Consolidated Reports**:
  - Daily, Weekly, and Monthly timeframes with date navigation and DatePicker.
  - 7-Day Spending Trend Bar Chart.
  - Multi-dimensional breakdown tabs: "By Category", "By Payment Mode", "By Bank".
- **Low-Confidence Review Screen**:
  - Review partial parses or ambiguous SMS messages (<75% confidence).
  - 1-tap confirmation, merchant/category editing, and automated category rule learning.
- **Transaction Details & Raw SMS Inspection**:
  - Edit merchant/payee, select from 10 standard expense categories, add notes, or inspect original SMS text.
- **Settings & Privacy Management**:
  - Excluded senders deny list manager.
  - Custom merchant keyword-to-category mapping rules.
  - Offline CSV export.

---

## 🛠️ How to Add a New Bank SMS Rule

1. Create a new Kotlin class in `app/src/main/java/com/example/smsexpensetracker/parser/rules/` implementing `BankSmsRule`:
```kotlin
package com.example.smsexpensetracker.parser.rules

import com.example.smsexpensetracker.data.model.*
import com.example.smsexpensetracker.parser.*

class MyNewBankRule : BankSmsRule {
    override val ruleId = "MY_NEW_BANK_RULE"
    override val bankName = "My New Bank"
    override val priority = 100

    private val debitPattern = Regex(
        """(?i)(?:INR|Rs\.?)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s*debited\s*from\s*(?:A\/c)?\s*(?<account>XX\d{4}|\d{4})\s*to\s*(?<merchant>[^.]+?)\.\s*(?:UPI\s*Ref:\s*(?<ref>\w+))?""",
        RegexOption.DOT_MATCHES_ALL
    )

    override fun canHandle(sender: String, body: String): Boolean {
        return sender.contains("MYBNK", ignoreCase = true) || body.contains("My New Bank", ignoreCase = true)
    }

    override fun parse(sender: String, body: String, timestamp: Long): ParseResult {
        val match = debitPattern.find(body) ?: return ParseResult.Failed("No match")
        val amount = ParserUtils.parseAmount(ParserUtils.safeGroup(match, "amount")) ?: return ParseResult.Failed("Invalid amount")
        val account = ParserUtils.cleanAccountLast4(ParserUtils.safeGroup(match, "account"))
        val merchant = ParserUtils.cleanMerchantName(ParserUtils.safeGroup(match, "merchant") ?: "Payee")
        val ref = ParserUtils.safeGroup(match, "ref")
        val category = CategoryClassifier.classify(merchant, body)

        return ParseResult.Success(
            Transaction(
                amount = amount,
                transactionType = TransactionType.DEBIT,
                bank = bankName,
                paymentMode = PaymentMode.UPI,
                merchantOrPayee = merchant,
                accountLast4 = account,
                referenceId = ref,
                timestamp = timestamp,
                rawSmsBody = body,
                confidence = 0.95f,
                category = category,
                sender = sender
            )
        )
    }
}
```

2. Register the rule in `RuleRegistry.kt`:
```kotlin
private val rules: List<BankSmsRule> = listOf(
    HdfcBankRule(),
    IciciBankRule(),
    MyNewBankRule(), // <-- Add here
    // ...
    GenericBankRule()
)
```

3. Add a corresponding test case in `BankSmsParserTest.kt` and run:
```bash
./gradlew testDebugUnitTest
```

---

## 🏗️ Building and Testing

### Prerequisites
- JDK 17 or JDK 21
- Android SDK Platform 35 / Build Tools 35.0.0

### Commands
```bash
# Run unit test suite (25+ tests covering parsing, deduplication, OTP filtering)
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Open source under Apache 2.0 License.
