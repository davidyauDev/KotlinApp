package com.example.myapplication.data.network

import com.example.myapplication.data.model.request.LoginRequest
import com.example.myapplication.data.model.response.AttendanceResponse
import com.example.myapplication.data.model.response.BannerResponse
import com.example.myapplication.data.model.response.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Multipart
    @POST("attendances")
    suspend fun sendAttendance(
        @Part("user_id") userId: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("notes") notes: RequestBody,
        @Part("device_model") deviceModel: RequestBody,
        @Part("battery_percentage") batteryPercentage: RequestBody,
        @Part("signal_strength") signalStrength: RequestBody,
        @Part("network_type") networkType: RequestBody,
        @Part("is_internet_available") isInternetAvailable: RequestBody,
        @Part("type") type: RequestBody,
        @Part("client_id") clientId: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<AttendanceResponse>

    @GET("banners")
    suspend fun getBanners(
        @Query("per_page") perPage: Int = 15,
        @Query("status") status: String? = null,
        @Query("valid") valid: Int? = null
    ): Response<BannerResponse>

}