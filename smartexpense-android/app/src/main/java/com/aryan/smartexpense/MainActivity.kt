package com.aryan.smartexpense

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

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
    private lateinit var tvTotalDebit: TextView
    private lateinit var tvTotalCredit: TextView
    private lateinit var btnResetMonth: TextView
    private lateinit var tvCategoryBreakdown: TextView
    private lateinit var tvMerchantList: TextView
    private lateinit var refundSection: LinearLayout
    private lateinit var tvRefundList: TextView
    private lateinit var tvHistoryList: TextView

    // SMS paste views
    private lateinit var etSmsPaste: EditText
    private lateinit var btnParseSms: Button
    private lateinit var tvParsedResult: TextView
    private lateinit var btnAddParsedEntry: Button

    // Upload statement views
    private lateinit var btnUploadPdf: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var etStatementPassword: EditText
    private lateinit var btnProcessFile: Button

    // Recent transactions views
    private lateinit var transactionsContainer: LinearLayout

    private val SMS_PERMISSION_CODE = 101
    private var authToken: String? = null
    private var categories: List<String> = emptyList()
    private var parsedExpense: Map<String, Any>? = null

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
        tvTotalDebit = findViewById(R.id.tvTotalDebit)
        tvTotalCredit = findViewById(R.id.tvTotalCredit)
        btnResetMonth = findViewById(R.id.btnResetMonth)
        tvCategoryBreakdown = findViewById(R.id.tvCategoryBreakdown)
        tvMerchantList = findViewById(R.id.tvMerchantList)
        refundSection = findViewById(R.id.refundSection)
        tvRefundList = findViewById(R.id.tvRefundList)
        tvHistoryList = findViewById(R.id.tvHistoryList)

        // SMS paste
        etSmsPaste = findViewById(R.id.etSmsPaste)
        btnParseSms = findViewById(R.id.btnParseSms)
        tvParsedResult = findViewById(R.id.tvParsedResult)
        btnAddParsedEntry = findViewById(R.id.btnAddParsedEntry)

        // Upload statement
        btnUploadPdf = findViewById(R.id.btnUploadPdf)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)
        etStatementPassword = findViewById(R.id.etStatementPassword)
        btnProcessFile = findViewById(R.id.btnProcessFile)

        // Recent transactions
        transactionsContainer = findViewById(R.id.transactionsContainer)
    }

    private fun setupListeners() {
        btnLoginTab.setOnClickListener {
            provideHapticFeedback()
            showLoginTab()
        }
        btnSignupTab.setOnClickListener {
            provideHapticFeedback()
            showSignupTab()
        }
        btnLogin.setOnClickListener {
            provideHapticFeedback()
            login()
        }
        btnSignup.setOnClickListener {
            provideHapticFeedback()
            signup()
        }
        btnRefresh.setOnClickListener {
            provideHapticFeedback()
            fetchAllData()
        }
        btnLogout.setOnClickListener {
            provideHapticFeedback()
            logout()
        }
        btnResetMonth.setOnClickListener {
            provideHapticFeedback()
            confirmResetMonth()
        }
        btnParseSms.setOnClickListener {
            provideHapticFeedback()
            parseSms()
        }
        btnAddParsedEntry.setOnClickListener {
            provideHapticFeedback()
            addParsedEntry()
        }

        btnUploadPdf.setOnClickListener {
            provideHapticFeedback()
            selectFile()
        }

        btnProcessFile.setOnClickListener {
            provideHapticFeedback()
            uploadFile()
        }
    }

    private fun provideHapticFeedback() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors (e.g., permission denied on some devices)
            e.printStackTrace()
        }
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
        val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

        authScreen.startAnimation(slideOut)
        authScreen.visibility = View.GONE

        dashboardScreen.startAnimation(slideIn)
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

        // Show loading indicator
        btnLogin.isEnabled = false
        btnLogin.text = "Testing connection..."

        lifecycleScope.launch {
            try {
                // First, test connectivity
                btnLogin.text = "Testing connection..."
                try {
                    val wakeResponse = RetrofitClient.apiService.wakeServer()
                    android.util.Log.d("SmartExpense", "Wake server response: ${wakeResponse.code()}")
                } catch (e: Exception) {
                    android.util.Log.e("SmartExpense", "Wake server failed", e)
                    runOnUiThread {
                        showError("Cannot connect to server.\nPlease check your internet connection.\n\nError: ${e.message}")
                    }
                    return@launch
                }

                // Wait for server to wake up
                btnLogin.text = "Waking server..."
                kotlinx.coroutines.delay(3000)

                // Now try login with retry logic
                var success = false
                var lastError: Exception? = null

                repeat(3) { attempt ->
                    try {
                        btnLogin.text = "Connecting... (Attempt ${attempt + 1}/3)"
                        android.util.Log.d("SmartExpense", "Login attempt ${attempt + 1}")
                        val response = RetrofitClient.apiService.login(mapOf(
                            "email" to email,
                            "password" to password
                        ))

                        android.util.Log.d("SmartExpense", "Login response code: ${response.code()}")

                        if (response.isSuccessful) {
                            val data = response.body()
                            val token = data?.get("token") as? String
                            val userEmail = data?.get("email") as? String
                            if (token != null) {
                                saveAuthToken(token)
                                saveUserEmail(userEmail ?: email)
                                authToken = token
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                                    showDashboard()
                                }
                                success = true
                                return@repeat
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            android.util.Log.e("SmartExpense", "Login failed: ${response.code()} - $errorBody")
                            lastError = Exception("HTTP ${response.code()}: $errorBody")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmartExpense", "Login attempt ${attempt + 1} failed", e)
                        lastError = e
                        if (attempt < 2) {
                            kotlinx.coroutines.delay(3000)
                        }
                    }
                }

                if (!success) {
                    runOnUiThread {
                        showError("Login failed after 3 attempts.\n\nLast error: ${lastError?.javaClass?.simpleName}\n${lastError?.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SmartExpense", "Login error", e)
                runOnUiThread {
                    showError("Error: ${e.javaClass.simpleName} - ${e.message}")
                }
            } finally {
                runOnUiThread {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                }
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

        // Show loading indicator
        btnSignup.isEnabled = false
        btnSignup.text = "Connecting..."

        lifecycleScope.launch {
            try {
                // First, try to wake up the server
                try {
                    RetrofitClient.apiService.wakeServer()
                } catch (e: Exception) {
                    // Ignore wake server errors
                }

                // Wait a moment for server to wake up
                kotlinx.coroutines.delay(2000)

                // Now try signup with retry logic
                var success = false
                var lastError: Exception? = null

                repeat(3) { attempt ->
                    try {
                        btnSignup.text = "Connecting... (Attempt ${attempt + 1}/3)"
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
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                                    showDashboard()
                                }
                                success = true
                                return@repeat
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            lastError = Exception("HTTP ${response.code()}: $errorBody")
                        }
                    } catch (e: Exception) {
                        lastError = e
                        if (attempt < 2) {
                            kotlinx.coroutines.delay(3000) // Wait 3 seconds before retry
                        }
                    }
                }

                if (!success) {
                    runOnUiThread {
                        showError("Registration failed after 3 attempts. Please try again.\n${lastError?.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showError("Error: ${e.javaClass.simpleName} - ${e.message}")
                }
            } finally {
                runOnUiThread {
                    btnSignup.isEnabled = true
                    btnSignup.text = "Create Account"
                }
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
        fetchTransactions()
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
                    var debitTotal = 0.0
                    if (categoryBreakdown != null && categoryBreakdown.isNotEmpty()) {
                        categoryBreakdown.forEach { (category, amount) ->
                            val amt = (amount as? Number)?.toDouble() ?: 0.0
                            debitTotal += amt
                            reportText.append("$category: ₹${String.format("%.2f", amt)}\n")
                        }
                    } else {
                        reportText.append("No expenses recorded for this month.")
                    }
                    tvCategoryBreakdown.text = reportText.toString()
                    tvTotalDebit.text = "₹${String.format("%.2f", debitTotal)}"
                    tvTotalCredit.text = "₹${String.format("%.2f", totalRefunds)}"

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

    private fun parseSms() {
        val smsText = etSmsPaste.text.toString()
        if (smsText.isBlank()) {
            Toast.makeText(this, "Please paste SMS text", Toast.LENGTH_SHORT).show()
            return
        }

        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.parseSms(ParseSmsRequest(smsText), "Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()
                    parsedExpense = mapOf(
                        "merchant" to (data?.merchant ?: ""),
                        "amount" to (data?.amount ?: 0.0),
                        "category" to (data?.category ?: ""),
                        "type" to (data?.type ?: "DEBIT")
                    )

                    val resultText = """
                        Merchant: ${data?.merchant}
                        Amount: ₹${data?.amount}
                        Category: ${data?.category}
                        Type: ${data?.type}
                    """.trimIndent()

                    tvParsedResult.text = resultText
                    tvParsedResult.visibility = View.VISIBLE
                    btnAddParsedEntry.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "SMS parsed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to parse SMS", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error parsing SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addParsedEntry() {
        val expense = parsedExpense ?: return
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addManualEntry(
                    expense,
                    "Bearer $token"
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Entry added successfully", Toast.LENGTH_SHORT).show()
                    etSmsPaste.text.clear()
                    tvParsedResult.text = ""
                    btnAddParsedEntry.visibility = View.GONE
                    parsedExpense = null
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

    private var selectedFileUri: Uri? = null
    private val FILE_PICK_CODE = 100

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "text/csv", "application/vnd.ms-excel"))
        }
        startActivityForResult(intent, FILE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileName(uri)
                val fileNameLower = fileName.lowercase()

                // Validate file type
                if (!fileNameLower.endsWith(".pdf") && !fileNameLower.endsWith(".csv")) {
                    Toast.makeText(this, "Invalid file type. Please select a PDF or CSV bank statement.", Toast.LENGTH_LONG).show()
                    return
                }

                selectedFileUri = uri
                tvSelectedFile.text = fileName
                tvSelectedFile.visibility = View.VISIBLE

                // Show password field only for PDF files
                if (fileNameLower.endsWith(".pdf")) {
                    etStatementPassword.visibility = View.VISIBLE
                } else {
                    etStatementPassword.visibility = View.GONE
                }

                btnProcessFile.visibility = View.VISIBLE
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
        if (result.isBlank()) result = "Selected file"
        return result
    }

    private fun uploadFile() {
        val uri = selectedFileUri ?: return
        val token = authToken ?: return

        btnProcessFile.isEnabled = false
        btnProcessFile.text = "Uploading..."

        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val fileName = getFileName(uri)
                    val password = etStatementPassword.text.toString()

                    val isPdf = fileName.lowercase().endsWith(".pdf")
                    val isCsv = fileName.lowercase().endsWith(".csv")

                    val url = when {
                        isPdf -> "https://smartexpenses-ft71.onrender.com/api/expenses/upload-pdf"
                        isCsv -> "https://smartexpenses-ft71.onrender.com/api/expenses/upload-csv"
                        else -> "https://smartexpenses-ft71.onrender.com/api/expenses/upload-pdf"
                    }

                    val requestBodyBuilder = okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("file", fileName, bytes.toRequestBody("application/octet-stream".toMediaType()))

                    // Add password for PDF if provided
                    if (isPdf && password.isNotBlank()) {
                        requestBodyBuilder.addFormDataPart("password", password)
                    }

                    val requestBody = requestBodyBuilder.build()

                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient.Builder()
                        .writeTimeout(300, TimeUnit.SECONDS)
                        .build()

                    val response = client.newCall(request).execute()

                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                            fetchAllData()
                            // Reset form
                            selectedFileUri = null
                            tvSelectedFile.visibility = View.GONE
                            etStatementPassword.text.clear()
                            etStatementPassword.visibility = View.GONE
                            btnProcessFile.visibility = View.GONE
                        } else {
                            Toast.makeText(this@MainActivity, "Upload failed: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                        btnProcessFile.isEnabled = true
                        btnProcessFile.text = "Upload & Process"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnProcessFile.isEnabled = true
                    btnProcessFile.text = "Upload & Process"
                }
            }
        }
    }

    private fun fetchTransactions() {
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTransactions("Bearer $token")
                if (response.isSuccessful) {
                    val transactions = response.body()
                    runOnUiThread {
                        displayTransactions(transactions ?: emptyList())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayTransactions(transactions: List<Map<String, Any>>) {
        transactionsContainer.removeAllViews()

        if (transactions.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No transactions yet."
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
            }
            transactionsContainer.addView(tv)
            return
        }

        for (tx in transactions) {
            val id = (tx["id"] as? Number)?.toLong() ?: 0L
            val merchant = tx["merchant"] as? String ?: ""
            val amount = tx["amount"] as? Number ?: 0.0
            val category = tx["category"] as? String ?: ""
            val type = tx["type"] as? String ?: "DEBIT"
            val date = tx["date"] as? String ?: ""

            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val detailsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val merchantTv = TextView(this).apply {
                text = merchant
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#F1F5F9"))
            }

            val metaTv = TextView(this).apply {
                val dateStr = date.toString().take(10)
                text = "$category • $dateStr"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
            }

            val actionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val amountTv = TextView(this).apply {
                text = if (type == "CREDIT") "+₹${"%.2f".format(amount)}" else "₹${"%.2f".format(amount)}"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(if (type == "CREDIT") android.graphics.Color.parseColor("#4ADE80") else android.graphics.Color.parseColor("#F1F5F9"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 0) }
            }

            val editBtn = Button(this).apply {
                text = "Edit"
                textSize = 12f
                setBackgroundColor(android.graphics.Color.parseColor("#2563EB"))
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    showEditDialog(id, merchant, amount as Double, category, type)
                }
            }

            detailsLayout.addView(merchantTv)
            detailsLayout.addView(metaTv)

            actionLayout.addView(amountTv)
            actionLayout.addView(editBtn)

            topRow.addView(detailsLayout)
            topRow.addView(actionLayout)

            itemView.addView(topRow)
            transactionsContainer.addView(itemView)
        }
    }

    private fun showEditDialog(id: Long, merchant: String, amount: Double, category: String, type: String) {
        val dialog = android.app.AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        
        val editMerchant = EditText(this).apply {
            setText(merchant)
            hint = "Merchant"
        }
        val editAmount = EditText(this).apply {
            setText(amount.toString())
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val editCategory = EditText(this).apply {
            setText(category)
            hint = "Category"
        }
        val editType = EditText(this).apply {
            setText(type)
            hint = "Type (DEBIT/CREDIT)"
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(editMerchant)
            addView(editAmount)
            addView(editCategory)
            addView(editType)
        }

        dialog.setView(container)
        dialog.setTitle("Edit Transaction")
        dialog.setPositiveButton("Save") { _, _ ->
            updateTransaction(id, editMerchant.text.toString(), editAmount.text.toString(), editCategory.text.toString(), editType.text.toString())
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun updateTransaction(id: Long, merchant: String, amount: String, category: String, type: String) {
        val token = authToken ?: return

        lifecycleScope.launch {
            try {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                val response = RetrofitClient.apiService.updateTransaction(
                    id,
                    mapOf(
                        "merchant" to merchant,
                        "amount" to amountValue,
                        "category" to category,
                        "type" to type
                    ),
                    "Bearer $token"
                )

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Transaction updated", Toast.LENGTH_SHORT).show()
                        fetchAllData()
                    } else {
                        Toast.makeText(this@MainActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
