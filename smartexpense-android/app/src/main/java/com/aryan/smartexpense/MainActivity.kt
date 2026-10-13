package com.aryan.smartexpense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvReport: TextView
    private lateinit var btnRefresh: Button

    private val SMS_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvReport = findViewById(R.id.tvReport)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Request SMS permission
        requestSmsPermission()

        // Check if user is logged in
        val token = getAuthToken()
        if (token != null) {
            btnLogin.text = "Logout"
            etEmail.isEnabled = false
            etPassword.isEnabled = false
            fetchReport()
        }

        btnLogin.setOnClickListener {
            val token = getAuthToken()
            if (token != null) {
                // Logout
                clearAuthToken()
                btnLogin.text = "Login"
                etEmail.isEnabled = true
                etPassword.isEnabled = true
                tvReport.text = "Please login to view expenses"
            } else {
                // Login
                login()
            }
        }

        btnRefresh.setOnClickListener {
            fetchReport()
        }
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun login() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(mapOf(
                    "email" to email,
                    "password" to password
                ))

                if (response.isSuccessful) {
                    val data = response.body()
                    val token = data?.get("token") as? String
                    if (token != null) {
                        saveAuthToken(token)
                        btnLogin.text = "Logout"
                        etEmail.isEnabled = false
                        etPassword.isEnabled = false
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        fetchReport()
                    }
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchReport() {
        val token = getAuthToken()
        if (token == null) {
            tvReport.text = "Please login first"
            return
        }

        val currentDate = java.util.Calendar.getInstance()
        val month = currentDate.get(java.util.Calendar.MONTH) + 1
        val year = currentDate.get(java.util.Calendar.YEAR)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getReport(month, year, "Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()
                    val breakdown = data?.get("categoryBreakdown") as? Map<*, *>
                    val reportText = StringBuilder()
                    reportText.append("Monthly Report ($month/$year)\n\n")

                    breakdown?.forEach { (category, amount) ->
                        reportText.append("$category: ₹$amount\n")
                    }

                    tvReport.text = reportText.toString()
                } else {
                    tvReport.text = "Failed to fetch report"
                }
            } catch (e: Exception) {
                tvReport.text = "Error: ${e.message}"
            }
        }
    }

    private fun getAuthToken(): String? {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    private fun saveAuthToken(token: String) {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
    }

    private fun clearAuthToken() {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        prefs.edit().remove("auth_token").apply()
    }
}
