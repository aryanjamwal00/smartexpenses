package com.aryan.smartexpense

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Auth views
    private lateinit var authScreen: LinearLayout
    private lateinit var dashboardScreen: LinearLayout
    private lateinit var btnLoginTab: TextView
    private lateinit var btnSignupTab: TextView
    private lateinit var loginForm: LinearLayout
    private lateinit var signupForm: LinearLayout
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etName: EditText
    private lateinit var etSignupEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etSignupPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button
    private lateinit var tvAuthError: TextView

    // Dashboard views
    private lateinit var tvUserEmail: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnLogout: Button
    private lateinit var tvTotalSpend: TextView
    private lateinit var tvRefundBadge: TextView
    private lateinit var btnResetMonth: TextView
    private lateinit var tvCategoryBreakdown: TextView
    private lateinit var tvMerchantList: TextView
    private lateinit var refundSection: LinearLayout
    private lateinit var tvRefundList: TextView
    private lateinit var tvHistoryList: TextView

    // Manual entry views
    private lateinit var etManualMerchant: EditText
    private lateinit var etManualAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerType: Spinner
    private lateinit var btnAddEntry: Button

    private val SMS_PERMISSION_CODE = 101
    private var authToken: String? = null
    private var categories: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        requestSmsPermission()
        checkAuthState()
        setupListeners()
    }

    private fun initializeViews() {
        // Auth screen
        authScreen = findViewById(R.id.authScreen)
        dashboardScreen = findViewById(R.id.dashboardScreen)
        btnLoginTab = findViewById(R.id.btnLoginTab)
        btnSignupTab = findViewById(R.id.btnSignupTab)
        loginForm = findViewById(R.id.loginForm)
        signupForm = findViewById(R.id.signupForm)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etName = findViewById(R.id.etName)
        etSignupEmail = findViewById(R.id.etSignupEmail)
        etPhone = findViewById(R.id.etPhone)
        etSignupPassword = findViewById(R.id.etSignupPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignup = findViewById(R.id.btnSignup)
        tvAuthError = findViewById(R.id.tvAuthError)

        // Dashboard
        tvUserEmail = findViewById(R.id.tvUserEmail)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnLogout = findViewById(R.id.btnLogout)
        tvTotalSpend = findViewById(R.id.tvTotalSpend)
        tvRefundBadge = findViewById(R.id.tvRefundBadge)
        btnResetMonth = findViewById(R.id.btnResetMonth)
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown)
        tvMerchantList = findViewById(R.id.tvMerchantList)
        refundSection = findViewById(R.id.refundSection)
        tvRefundList = findViewById(R.id.tvRefundList)
        tvHistoryList = findViewById(R.id.tvHistoryList)

        // Manual entry
        etManualMerchant = findViewById(R.id.etManualMerchant)
        etManualAmount = findViewById(R.id.etManualAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerType = findViewById(R.id.spinnerType)
        btnAddEntry = findViewById(R.id.btnAddEntry)

        // Setup type spinner
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Expense (Debit)", "Refund/Income (Credit)"))
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter
    }

    private fun setupListeners() {
        btnLoginTab.setOnClickListener { showLoginTab() }
        btnSignupTab.setOnClickListener { showSignupTab() }
        btnLogin.setOnClickListener { login() }
        btnSignup.setOnClickListener { signup() }
        btnRefresh.setOnClickListener { fetchAllData() }
        btnLogout.setOnClickListener { logout() }
        btnResetMonth.setOnClickListener { confirmResetMonth() }
        btnAddEntry.setOnClickListener { addManualEntry() }
    }

    private fun showLoginTab() {
        btnLoginTab.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
        btnLoginTab.setTextColor(getColor(android.R.color.white))
        btnSignupTab.setBackgroundColor(getColor(android.R.color.transparent))
        btnSignupTab.setTextColor(getColor(android.R.color.darker_gray))
        loginForm.visibility = View.VISIBLE
        signupForm.visibility = View.GONE
    }

    private fun showSignupTab() {
        btnSignupTab.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
        btnSignupTab.setTextColor(getColor(android.R.color.white))
        btnLoginTab.setBackgroundColor(getColor(android.R.color.transparent))
        btnLoginTab.setTextColor(getColor(android.R.color.darker_gray))
        signupForm.visibility = View.VISIBLE
        loginForm.visibility = View.GONE
    }

    private fun checkAuthState() {
        authToken = getAuthToken()
        if (authToken != null) {
            showDashboard()
        } else {
            showAuthScreen()
        }
    }

    private fun showAuthScreen() {
        authScreen.visibility = View.VISIBLE
        dashboardScreen.visibility = View.GONE
    }

    private fun showDashboard() {
        authScreen.visibility = View.GONE
        dashboardScreen.visibility = View.VISIBLE
        tvUserEmail.text = getUserEmail() ?: "user@example.com"
        fetchAllData()
    }

    private fun login() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if (email.isBlank() || password.isBlank()) {
            showError("Please enter email and password")
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
                    val userEmail = data?.get("email") as? String
                    if (token != null) {
                        saveAuthToken(token)
                        saveUserEmail(userEmail ?: email)
                        authToken = token
                        Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                        showDashboard()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorMap = org.json.JSONObject(errorBody)
                        errorMap.optString("message", "Login failed")
                    } catch (e: Exception) {
                        "Login failed"
                    }
                    showError(errorMessage)
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun signup() {
        val name = etName.text.toString()
        val email = etSignupEmail.text.toString()
        val phone = etPhone.text.toString()
        val password = etSignupPassword.text.toString()

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            showError("Please fill all required fields")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(mapOf(
                    "name" to name,
                    "email" to email,
                    "phoneNumber" to phone,
                    "password" to password
                ))

                if (response.isSuccessful) {
                    val data = response.body()
                    val token = data?.get("token") as? String
                    val userEmail = data?.get("email") as? String
                    if (token != null) {
                        saveAuthToken(token)
                        saveUserEmail(userEmail ?: email)
                        authToken = token
                        Toast.makeText(this@MainActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                        showDashboard()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorMap = org.json.JSONObject(errorBody)
                        errorMap.optString("message", "Registration failed")
                    } catch (e: Exception) {
                        "Registration failed"
                    }
                    showError(errorMessage)
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun logout() {
        clearAuthToken()
        authToken = null
        showAuthScreen()
    }

    private fun showError(message: String) {
        tvAuthError.text = message
        tvAuthError.visibility = View.VISIBLE
    }

    private fun fetchAllData() {
        fetchReport()
        fetchHistory()
        fetchCategories()
    }

    private fun fetchReport() {
        val token = authToken ?: return

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getReport(month, year, "Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()
                    val categoryBreakdown = data?.get("categoryBreakdown") as? Map<*, *>
                    val merchantBreakdown = data?.get("merchantBreakdown") as? List<*>
                    val totalRefunds = data?.get("totalRefunds") as? Double ?: 0.0
                    val refundDetails = data?.get("refundDetails") as? List<*>

                    // Category breakdown
                    val reportText = StringBuilder()
                    var total = 0.0
                    if (categoryBreakdown != null && categoryBreakdown.isNotEmpty()) {
                        categoryBreakdown.forEach { (category, amount) ->
                            val amt = (amount as? Number)?.toDouble() ?: 0.0
                            total += amt
                            reportText.append("$category: ₹${String.format("%.2f", amt)}\n")
                        }
                    } else {
                        reportText.append("No expenses recorded for this month.")
                    }
                    tvCategoryBreakdown.text = reportText.toString()
                    tvTotalSpend.text = "₹${String.format("%.2f", total)}"

                    // Refund badge
                    if (totalRefunds > 0) {
                        tvRefundBadge.visibility = View.VISIBLE
                        tvRefundBadge.text = "Refunds: ₹${String.format("%.2f", totalRefunds)}"
                    } else {
                        tvRefundBadge.visibility = View.GONE
                    }

                    // Merchant breakdown
                    val merchantText = StringBuilder()
                    if (merchantBreakdown != null && merchantBreakdown.isNotEmpty()) {
                        merchantBreakdown.forEach { item ->
                            val merchant = (item as? Map<*, *>)?.get("merchant") as? String ?: ""
                            val category = (item as? Map<*, *>)?.get("category") as? String ?: ""
                            val amount = (item as? Map<*, *>)?.get("amount") as? Number ?: 0
                            merchantText.append("$merchant ($category): ₹${String.format("%.2f", amount.toDouble())}\n")
                        }
                    } else {
                        merchantText.append("No merchant data available.")
                    }
                    tvMerchantList.text = merchantText.toString()

                    // Refund details
                    if (refundDetails != null && refundDetails.isNotEmpty()) {
                        refundSection.visibility = View.VISIBLE
                        val refundText = StringBuilder()
                        refundDetails.forEach { item ->
                            val merchant = (item as? Map<*, *>)?.get("merchant") as? String ?: ""
                            val amount = (item as? Map<*, *>)?.get("amount") as? Number ?: 0
                            val dateStr = (item as? Map<*, *>)?.get("date") as? String ?: ""
                            refundText.append("$merchant: +₹${String.format("%.2f", amount.toDouble())}\n")
                        }
                        tvRefundList.text = refundText.toString()
                    } else {
                        refundSection.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchHistory() {
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getHistory("Bearer $token")
                if (response.isSuccessful) {
                    val history = response.body() as? List<*>
                    val historyText = StringBuilder()
                    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                    if (history != null && history.isNotEmpty()) {
                        history.forEach { item ->
                            val month = (item as? Map<*, *>)?.get("month") as? Number ?: 0
                            val year = (item as? Map<*, *>)?.get("year") as? Number ?: 0
                            val total = (item as? Map<*, *>)?.get("total") as? Number ?: 0
                            val monthIndex = month.toInt() - 1
                            if (monthIndex in monthNames.indices) {
                                historyText.append("${monthNames[monthIndex]} $year: ₹${String.format("%.2f", total.toDouble())}\n")
                            }
                        }
                    } else {
                        historyText.append("No past records available.")
                    }
                    tvHistoryList.text = historyText.toString()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCategories() {
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCategories("Bearer $token")
                if (response.isSuccessful) {
                    val cats = response.body() as? List<*>
                    categories = cats?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String } ?: emptyList()

                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, listOf("Auto-detect category") + categories)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerCategory.adapter = adapter
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmResetMonth() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        AlertDialog.Builder(this)
            .setTitle("Reset Month")
            .setMessage("Are you sure you want to reset expenses for Month $month/$year? This cannot be undone.")
            .setPositiveButton("Yes") { _, _ -> resetMonth(month, year) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun resetMonth(month: Int, year: Int) {
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.resetMonth(month, year, "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Current month expenses have been reset.", Toast.LENGTH_SHORT).show()
                    fetchAllData()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to reset expenses.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error resetting month: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addManualEntry() {
        val merchant = etManualMerchant.text.toString()
        val amountStr = etManualAmount.text.toString()
        val category = spinnerCategory.selectedItem?.toString()
        val typeStr = spinnerType.selectedItem?.toString()

        if (merchant.isBlank() || amountStr.isBlank()) {
            Toast.makeText(this, "Please enter merchant name and amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (typeStr?.contains("Credit") == true) "CREDIT" else "DEBIT"
        val selectedCategory = if (category == "Auto-detect category") null else category

        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val entryMap = mutableMapOf<String, Any>(
                    "merchant" to merchant,
                    "amount" to amount,
                    "type" to type
                )
                if (selectedCategory != null) {
                    entryMap["category"] = selectedCategory
                }

                val response = RetrofitClient.apiService.addManualEntry(
                    entryMap,
                    "Bearer $token"
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Entry added successfully", Toast.LENGTH_SHORT).show()
                    etManualMerchant.text.clear()
                    etManualAmount.text.clear()
                    spinnerCategory.setSelection(0)
                    fetchAllData()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to add entry", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error adding entry: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    private fun getAuthToken(): String? {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    private fun saveAuthToken(token: String) {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
    }

    private fun saveUserEmail(email: String) {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    private fun getUserEmail(): String? {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        return prefs.getString("user_email", null)
    }

    private fun clearAuthToken() {
        val prefs = getSharedPreferences("SmartExpense", MODE_PRIVATE)
        prefs.edit().remove("auth_token").apply()
    }
}
