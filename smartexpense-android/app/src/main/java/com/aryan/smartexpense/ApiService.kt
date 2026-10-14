package com.aryan.smartexpense

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class ParseSmsRequest(
    val message: String
)

data class ParseSmsResponse(
    val amount: Double,
    val merchant: String,
    val category: String,
    val type: String,
    val exceeded: Boolean,
    val alert: String
)

interface ApiService {
    @POST("/api/expenses/parse-sms")
    suspend fun parseSms(
        @Body request: ParseSmsRequest,
        @Header("Authorization") token: String? = null
    ): Response<ParseSmsResponse>

    @POST("/api/auth/login")
    suspend fun login(
        @Body credentials: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("/api/auth/register")
    suspend fun register(
        @Body userData: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("/api/expenses/report")
    suspend fun getReport(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @GET("/api/expenses/history")
    suspend fun getHistory(
        @Header("Authorization") token: String
    ): Response<List<Map<String, Any>>>

    @GET("/api/expenses/categories")
    suspend fun getCategories(
        @Header("Authorization") token: String
    ): Response<List<Map<String, Any>>>

    @DELETE("/api/expenses/reset")
    suspend fun resetMonth(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Header("Authorization") token: String
    ): Response<Void>

    @POST("/api/expenses/manual-entry")
    suspend fun addManualEntry(
        @Body entry: Map<String, Any>,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @GET("/api/expenses/transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String
    ): Response<List<Map<String, Any>>>

    @PUT("/api/expenses/update/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body entry: Map<String, Any>,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @GET("/")
    suspend fun wakeServer(): Response<Unit>

    @Multipart
    @POST("/api/expenses/upload-pdf")
    suspend fun uploadPdf(
        @Part file: okhttp3.MultipartBody.Part,
        @Part password: okhttp3.MultipartBody.Part?,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @Multipart
    @POST("/api/expenses/upload-csv")
    suspend fun uploadCsv(
        @Part file: okhttp3.MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
}
