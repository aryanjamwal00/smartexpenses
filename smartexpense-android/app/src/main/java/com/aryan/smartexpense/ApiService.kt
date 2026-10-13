package com.aryan.smartexpense

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

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
}
