package com.example.myapplication.data.model.response

data class BannerResponse(
    val data: List<Banner>
)

data class Banner(
    val id: Int,
    val title: String,
    val image_url: String,
    val status: String,
    val start_at: String,
    val end_at: String,
    val created_at: String,
    val updated_at: String
)