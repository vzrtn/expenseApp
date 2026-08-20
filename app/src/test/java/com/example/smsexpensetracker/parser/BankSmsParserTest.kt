package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.data.model.PaymentMode
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    private val timestamp = 1755678000000L // Synthetic test timestamp

    @Test
    fun testHdfcBankDebitSms() {
        val sender = "AD-HDFCBK"
        val sms = "Rs.500.00 debited from A/c XX1234 on 20-Aug-26 to VPA swiggy@ybl UPI Ref No 123456789. Avl Bal: INR 12,345"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue("Expected ParseResult.Success, got $result", result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(500.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("HDFC Bank", tx.bank)
        assertEquals("1234", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
        assertEquals(PaymentMode.UPI, tx.paymentMode)
    }

    @Test
    fun testHdfcBankCardDebitSms() {
        val sender = "VM-HDFCBK"
        val sms = "Spent INR 3,499.00 on HDFC Bank Card XX9012 at AMAZON on 20-Aug-26. Avail Lmt: INR 85,000."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(3499.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("HDFC Bank", tx.bank)
        assertEquals("9012", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_SHOPPING, tx.category)
    }

    @Test
    fun testIciciBankDebitSms() {
        val sender = "VM-ICICIB"
        val sms = "Acct XX5678 debited with INR 750.00 on 20-Aug-26. Info: ZOMATO. UPI:987654321. Avl Bal: INR 25,000."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(750.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("ICICI Bank", tx.bank)
        assertEquals("5678", tx.accountLast4)
        assertEquals("987654321", tx.referenceId)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
    }

    @Test
    fun testIciciBankCardDebitSms() {
        val sender = "VK-ICICIB"
        val sms = "Dear Customer, INR 1,299.00 spent on ICICI Bank Card XX4321 on 20-Aug-26 at FLIPKART."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(1299.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("ICICI Bank", tx.bank)
        assertEquals("4321", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_SHOPPING, tx.category)
    }

    @Test
    fun testSbiBankDebitSms() {
        val sender = "JD-SBIINB"
        val sms = "Dear SBI User, A/c 9012 debited by Rs450.00 on 20Aug26 trf to BLINKIT Ref 888777666555. Bal:Rs 10,000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(450.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("SBI", tx.bank)
        assertEquals("9012", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_GROCERIES, tx.category)
    }

    @Test
    fun testSbiCreditCardDebitSms() {
        val sender = "AX-SBICRD"
        val sms = "INR 350.00 spent on your SBI Credit Card ending 9012 on 20-Aug-26 at AMAZON. Avail Lmt: Rs.45,000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(350.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("SBI", tx.bank)
        assertEquals("9012", tx.accountLast4)
        assertEquals(PaymentMode.CREDIT_CARD, tx.paymentMode)
        assertEquals(CategoryClassifier.CATEGORY_SHOPPING, tx.category)
    }

    @Test
    fun testAxisBankDebitSms() {
        val sender = "BZ-AXISBK"
        val sms = "INR 850.00 debited from A/c no. XX1234 on 20-Aug-26 to UBER. UPI Ref 11223344. Avl Bal: INR 15,000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(850.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("Axis Bank", tx.bank)
        assertEquals("1234", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_TRANSPORT, tx.category)
    }

    @Test
    fun testKotakBankDebitSms() {
        val sender = "BW-KOTAKB"
        val sms = "Sent Rs.499.00 from Kotak Bank AC XX9999 to NETFLIX on 20-Aug-26. Ref 99887766."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(499.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("Kotak Bank", tx.bank)
        assertEquals("9999", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_ENTERTAINMENT, tx.category)
    }

    @Test
    fun testPnbBankDebitSms() {
        val sender = "VK-PNBSMS"
        val sms = "A/C ****4321 Debited by Rs.650.00 on 20-Aug-26 via UPI to ZEPTO. Ref:445566. Bal:Rs.10000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(650.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("Punjab National Bank", tx.bank)
        assertEquals("4321", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_GROCERIES, tx.category)
    }

    @Test
    fun testBankOfBarodaDebitSms() {
        val sender = "MD-BOBSMS"
        val sms = "Your A/C 7788 has been debited by INR 350.00 on 20-Aug-26 towards AIRTEL. Total Bal: INR 8500"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(350.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("Bank of Baroda", tx.bank)
        assertEquals("7788", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_BILLS, tx.category)
    }

    @Test
    fun testIdfcFirstBankDebitSms() {
        val sender = "CP-IDFCFB"
        val sms = "Paid INR 1,250.00 from IDFC FIRST Bank A/C ending 5544 to APOLLO PHARMACY on 20-Aug-26. UPI Ref: 123456789"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(1250.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("IDFC FIRST Bank", tx.bank)
        assertEquals("5544", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_HEALTH, tx.category)
    }

    @Test
    fun testYesBankDebitSms() {
        val sender = "AD-YESBNK"
        val sms = "INR 450.00 debited from YES Bank A/c ending 1122 on 20-Aug-26 towards BOOKMYSHOW. Avl Bal: INR 12,000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(450.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("YES Bank", tx.bank)
        assertEquals("1122", tx.accountLast4)
        assertEquals(CategoryClassifier.CATEGORY_ENTERTAINMENT, tx.category)
    }

    @Test
    fun testPhonePeDebitSms() {
        val sender = "AX-PAYTM"
        val sms = "Paid Rs.199.00 to Swiggy on PhonePe. UPI Ref: 123456789012."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(199.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("PhonePe", tx.paymentApp)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
    }

    @Test
    fun testCreditTransactionSms() {
        val sender = "AD-HDFCBK"
        val sms = "Rs 1,500.00 credited to HDFC Bank A/c XX1234 on 20-Aug-26 by John Doe Ref 12345. Bal: INR 20,000"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(1500.00, tx.amount, 0.001)
        assertEquals(TransactionType.CREDIT, tx.transactionType)
        assertEquals("HDFC Bank", tx.bank)
    }

    @Test
    fun testOtpMessageIsIgnored() {
        val sender = "AD-HDFCBK"
        val sms = "123456 is your secret OTP for HDFC NetBanking login. Do not share with anyone."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue("Expected ParseResult.Ignored, got $result", result is ParseResult.Ignored)
    }

    @Test
    fun testPromotionalMessageIsIgnored() {
        val sender = "AD-HDFCBK"
        val sms = "Congratulations! You are eligible for a Pre-Approved Personal Loan of up to Rs 5 Lakhs. Click to apply."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun testExcludedSenderIsIgnored() {
        val sender = "AD-PERSONAL"
        val sms = "Rs.500.00 debited from A/c XX1234 on 20-Aug-26 to VPA swiggy@ybl"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp, excludedSenders = setOf("AD-PERSONAL"))
        assertTrue(result is ParseResult.Ignored)
    }

    @Test
    fun testFuzzyDeduplication() {
        val tx1 = Transaction(
            id = 1,
            amount = 500.00,
            transactionType = TransactionType.DEBIT,
            bank = "HDFC Bank",
            paymentMode = PaymentMode.UPI,
            paymentApp = null,
            merchantOrPayee = "Swiggy",
            accountLast4 = "1234",
            referenceId = "998877",
            timestamp = timestamp,
            rawSmsBody = "Bank debit alert",
            confidence = 0.95f,
            category = "Food & Dining",
            sender = "AD-HDFCBK"
        )

        // Duplicate from PhonePe sent 30 seconds later for the exact same amount and reference
        val tx2 = Transaction(
            id = 0,
            amount = 500.00,
            transactionType = TransactionType.DEBIT,
            bank = "PhonePe",
            paymentMode = PaymentMode.UPI,
            paymentApp = "PhonePe",
            merchantOrPayee = "Swiggy",
            accountLast4 = null,
            referenceId = "998877",
            timestamp = timestamp + 30_000L,
            rawSmsBody = "PhonePe confirmation",
            confidence = 0.95f,
            category = "Food & Dining",
            sender = "VK-PHONEPE"
        )

        assertTrue(FuzzyDeduplicator.isDuplicate(tx2, tx1))
    }

    @Test
    fun testGooglePayDebitSms() {
        val sender = "AD-GPAY"
        val sms = "Paid Rs.500.00 to Starbucks using Google Pay UPI ID merchant@okhdfcbank."

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(500.00, tx.amount, 0.001)
        assertEquals("Google Pay", tx.paymentApp)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
    }

    @Test
    fun testPaytmDebitSms() {
        val sender = "AX-PAYTM"
        val sms = "Paid Rs.350 on Paytm to Dominos Pizza. Txn ID: 987654321"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(350.00, tx.amount, 0.001)
        assertEquals("Paytm", tx.paymentApp)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
    }

    @Test
    fun testGenericBankFallbackDebitSms() {
        val sender = "VK-CANBNK"
        val sms = "Rs.750.00 debited from A/c XX9988 on 20-Aug-26 to ZOMATO UPI ref 12345678"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(750.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.transactionType)
        assertEquals("9988", tx.accountLast4)
        assertEquals("Canara Bank", tx.bank)
        assertEquals(CategoryClassifier.CATEGORY_FOOD, tx.category)
    }

    @Test
    fun testCustomCategoryUserMappingOverride() {
        val sender = "AD-HDFCBK"
        val sms = "Rs.250.00 debited from A/c XX1234 on 20-Aug-26 to VPA localshop@upi"

        // Without override, localshop@upi is Others
        val result1 = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result1 is ParseResult.Success)
        assertEquals(CategoryClassifier.CATEGORY_OTHERS, (result1 as ParseResult.Success).transaction.category)

        // With custom override mapping "localshop" -> "Groceries"
        val customMap = mapOf("localshop" to CategoryClassifier.CATEGORY_GROCERIES)
        val result2 = SmsParsingEngine.parseSms(sender, sms, timestamp, userCustomCategoryMappings = customMap)
        assertTrue(result2 is ParseResult.Success)
        assertEquals(CategoryClassifier.CATEGORY_GROCERIES, (result2 as ParseResult.Success).transaction.category)
    }

    @Test
    fun testInvestmentCategoryDetection() {
        val sender = "VM-HDFCBK"
        val sms = "Rs 5,000.00 debited from A/c XX1234 on 20-Aug-26 to ZERODHA BROKING UPI Ref 991122"

        val result = SmsParsingEngine.parseSms(sender, sms, timestamp)
        assertTrue(result is ParseResult.Success)

        val tx = (result as ParseResult.Success).transaction
        assertEquals(5000.00, tx.amount, 0.001)
        assertEquals(CategoryClassifier.CATEGORY_INVESTMENTS, tx.category)
    }
}
