package com.aryan.smartexpense

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"

        // OTP keywords to block (STRICT - any match blocks the message)
        private val OTP_KEYWORDS = listOf(
            "otp", "OTP", "verification", "verify", "code", "CODE",
            "pin", "PIN", "password", "PASSWORD", "auth", "AUTH",
            "one time", "onetime", "2fa", "mfa", "secure", "security",
            "expire", "expiry", "valid for", "validity", "confirm",
            "authentication", "authenticate", "login", "sign in"
        )

        // Transaction keywords to allow (must contain at least one)
        private val TRANSACTION_KEYWORDS = listOf(
            "debited", "credit", "spent", "paid", "purchase", "bought",
            "transaction", "transfer", "withdrawn", "deposit", "refund",
            "spent at", "paid to", "bought from", "charged", "rs.",
            "inr", "amount", "balance", "account"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<ByteArray>
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu)
                        val messageBody = sms.messageBody
                        val sender = sms.originatingAddress

                        Log.d(TAG, "SMS received from: $sender")
                        Log.d(TAG, "Message: $messageBody")

                        // Filter OTP messages
                        if (isOtpMessage(messageBody)) {
                            Log.d(TAG, "OTP message blocked")
                            return
                        }

                        // Only process transaction messages
                        if (isTransactionMessage(messageBody)) {
                            Log.d(TAG, "Transaction message detected")
                            // Send to backend
                            sendToBackend(context, messageBody, sender)
                        } else {
                            Log.d(TAG, "Non-transaction message ignored")
                        }
                    }
                }
            }
        }
    }

    private fun isOtpMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return OTP_KEYWORDS.any { lowerMessage.contains(it.lowercase()) }
    }

    private fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return TRANSACTION_KEYWORDS.any { lowerMessage.contains(it.lowercase()) }
    }

    private fun sendToBackend(context: Context, message: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("SmartExpense", Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", null)

                if (token == null) {
                    Log.d(TAG, "User not logged in, skipping SMS processing")
                    return
                }

                val apiService = RetrofitClient.apiService
                val response = apiService.parseSms(message, "Bearer $token")

                if (response.isSuccessful) {
                    Log.d(TAG, "SMS sent to backend successfully")
                } else {
                    Log.e(TAG, "Failed to send SMS to backend: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS to backend", e)
            }
        }
    }
}
